package com.smart.transformer.service;

import com.smart.transformer.dto.request.FailureCostRequest;
import com.smart.transformer.dto.request.LoadSimulationRequest;
import com.smart.transformer.dto.response.*;
import com.smart.transformer.entity.Transformer;
import com.smart.transformer.entity.enums.AlertSeverity;
import com.smart.transformer.entity.enums.TransformerStatus;
import com.smart.transformer.repository.AlertRepository;
import com.smart.transformer.repository.MaintenanceRecordRepository;
import com.smart.transformer.repository.TransformerRepository;
import com.smart.transformer.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 3 "Decision Support" backend logic:
 *  - Failure Cost Calculator
 *  - What-if Load Simulator
 *  - Fleet Risk Heatmap
 *  - Transformer Comparison
 *
 * These are deterministic calculations grounded in real fleet data, with an
 * optional OpenAI-written narrative layered on top for human-readable takeaways.
 */
@Service
@RequiredArgsConstructor
public class DecisionSupportService {

    private final TransformerRepository transformerRepository;
    private final AlertRepository alertRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final TransformerService transformerService;
    private final SensorReadingService sensorReadingService;
    private final OpenAiService openAiService;

    // ---------- Failure Cost Calculator ----------

    public FailureCostResponse calculateFailureCost(FailureCostRequest request) {
        Transformer transformer = transformerService.getEntity(request.getTransformerId());

        double healthScore = transformer.getHealthScore() != null ? transformer.getHealthScore() : 70.0;
        // Lower health score => higher failure probability. Health score is assumed 0-100.
        double failureProbability = clamp((100.0 - healthScore) / 100.0, 0.01, 0.99);

        double totalFailureCost = request.getReplacementCost()
                + (request.getDowntimeCostPerHour() * request.getEstimatedDowntimeHours());
        double expectedCostOfFailure = totalFailureCost * failureProbability;
        double preventiveCost = request.getPreventiveMaintenanceCost() != null ? request.getPreventiveMaintenanceCost() : 0.0;
        double netSavings = expectedCostOfFailure - preventiveCost;

        String recommendation = netSavings > 0
                ? String.format("Preventive maintenance is cost-justified: expected failure cost (%.2f) "
                        + "exceeds preventive cost (%.2f) by %.2f.", expectedCostOfFailure, preventiveCost, netSavings)
                : String.format("Preventive maintenance cost (%.2f) currently exceeds the expected failure "
                        + "cost (%.2f); monitor and re-evaluate as health score changes.", preventiveCost, expectedCostOfFailure);

        return new FailureCostResponse(
                transformer.getId(), transformer.getName(), round2(failureProbability),
                round2(totalFailureCost), round2(expectedCostOfFailure), round2(preventiveCost),
                round2(netSavings), recommendation);
    }

    // ---------- What-if Load Simulator ----------

    public LoadSimulationResponse simulateLoad(LoadSimulationRequest request) {
        Transformer transformer = transformerService.getEntity(request.getTransformerId());
        SensorReadingResponse latest = sensorReadingService.getLatest(transformer.getId());

        double baselineLoad = latest != null && latest.getLoadCurrentAmps() != null ? latest.getLoadCurrentAmps() : 0.0;
        double baselineTemp = latest != null && latest.getTemperatureCelsius() != null ? latest.getTemperatureCelsius() : 25.0;
        double baselineHealth = transformer.getHealthScore() != null ? transformer.getHealthScore() : 70.0;

        double loadMultiplier = 1.0 + (request.getLoadChangePercent() / 100.0);
        double simulatedLoad = Math.max(0, baselineLoad * loadMultiplier);

        // Simplified thermal model: temperature rise roughly tracks load^2 (I^2R heating),
        // plus any simulated ambient temperature delta. This is intentionally approximate —
        // good enough for a "what-if" directional estimate, not a precision thermal simulation.
        double loadRatioSquared = baselineLoad > 0 ? Math.pow(simulatedLoad / baselineLoad, 2) : Math.pow(loadMultiplier, 2);
        double ambientDelta = request.getAmbientTempDeltaCelsius() != null ? request.getAmbientTempDeltaCelsius() : 0.0;
        double simulatedTemp = (baselineTemp * loadRatioSquared) + ambientDelta;

        // Health score penalty scales with excess load beyond baseline and temperature rise.
        double loadPenalty = Math.max(0, request.getLoadChangePercent()) * 0.4;
        double tempPenalty = Math.max(0, simulatedTemp - baselineTemp) * 0.6;
        double simulatedHealth = clamp(baselineHealth - loadPenalty - tempPenalty, 0, 100);

        TransformerStatus baselineStatus = transformer.getStatus();
        TransformerStatus simulatedStatus = deriveStatusFromHealth(simulatedHealth);

        String narrative = generateLoadNarrative(transformer, baselineLoad, simulatedLoad,
                baselineTemp, simulatedTemp, baselineHealth, simulatedHealth, simulatedStatus);

        return new LoadSimulationResponse(
                transformer.getId(), transformer.getName(),
                round2(baselineLoad), round2(simulatedLoad),
                round2(baselineTemp), round2(simulatedTemp),
                round2(baselineHealth), round2(simulatedHealth),
                baselineStatus, simulatedStatus,
                narrative);
    }

    private String generateLoadNarrative(Transformer t, double baseLoad, double simLoad, double baseTemp,
                                          double simTemp, double baseHealth, double simHealth,
                                          TransformerStatus simStatus) {
        String prompt = String.format(
                "Transformer %s (%s). Baseline load %.1fA -> simulated load %.1fA. "
                        + "Baseline temperature %.1fC -> simulated %.1fC. "
                        + "Baseline health score %.1f -> simulated %.1f (status would become %s). "
                        + "In 2-3 sentences, explain the risk implication of this change for a field engineer "
                        + "deciding whether to approve this load change.",
                t.getName(), t.getAssetTag(), baseLoad, simLoad, baseTemp, simTemp, baseHealth, simHealth, simStatus);
        return openAiService.complete(
                "You are a risk-assessment assistant for electrical transformer loading decisions.",
                prompt);
    }

    // ---------- Fleet Risk Heatmap ----------

    public List<FleetRiskPointResponse> getFleetRiskHeatmap() {
        List<FleetRiskPointResponse> points = new ArrayList<>();
        for (Transformer t : transformerRepository.findAll()) {
            long criticalAlerts = alertRepository.countByTransformerIdAndAcknowledgedFalseAndSeverity(t.getId(), AlertSeverity.CRITICAL);
            long warningAlerts = alertRepository.countByTransformerIdAndAcknowledgedFalseAndSeverity(t.getId(), AlertSeverity.WARNING);
            double healthScore = t.getHealthScore() != null ? t.getHealthScore() : 100.0;

            double riskScore = clamp((100.0 - healthScore) + (criticalAlerts * 15.0) + (warningAlerts * 5.0), 0, 100);

            points.add(new FleetRiskPointResponse(
                    t.getId(), t.getAssetTag(), t.getName(), t.getLocation(),
                    t.getLatitude(), t.getLongitude(), t.getStatus(),
                    t.getHealthScore(), round2(riskScore), criticalAlerts, warningAlerts));
        }
        points.sort((a, b) -> Double.compare(b.getRiskScore(), a.getRiskScore()));
        return points;
    }

    // ---------- Transformer Comparison ----------

    public TransformerComparisonResponse compare(List<Long> transformerIds) {
        List<TransformerResponse> transformers = new ArrayList<>();
        List<SensorReadingResponse> latestReadings = new ArrayList<>();
        List<Long> maintenanceCounts = new ArrayList<>();
        List<Long> openAlertCounts = new ArrayList<>();

        for (Long id : transformerIds) {
            Transformer t = transformerService.getEntity(id);
            transformers.add(EntityMapper.toResponse(t));
            latestReadings.add(sensorReadingService.getLatest(id));
            maintenanceCounts.add(maintenanceRecordRepository.countByTransformerId(id));
            openAlertCounts.add(alertRepository.countByTransformerIdAndAcknowledgedFalse(id));
        }

        String verdict = generateComparisonVerdict(transformers, latestReadings, maintenanceCounts, openAlertCounts);

        return new TransformerComparisonResponse(transformers, latestReadings, maintenanceCounts, openAlertCounts, verdict);
    }

    private String generateComparisonVerdict(List<TransformerResponse> transformers, List<SensorReadingResponse> readings,
                                               List<Long> maintenanceCounts, List<Long> openAlertCounts) {
        StringBuilder sb = new StringBuilder("Compare these transformers and identify which is healthiest and "
                + "which needs the most attention, with a one-sentence reason for each:\n");
        for (int i = 0; i < transformers.size(); i++) {
            TransformerResponse t = transformers.get(i);
            SensorReadingResponse r = readings.get(i);
            sb.append("- ").append(t.getName()).append(" (").append(t.getAssetTag()).append("): ")
                    .append("status=").append(t.getStatus())
                    .append(", health_score=").append(t.getHealthScore())
                    .append(", open_alerts=").append(openAlertCounts.get(i))
                    .append(", maintenance_records=").append(maintenanceCounts.get(i));
            if (r != null) {
                sb.append(", latest_temp=").append(r.getTemperatureCelsius())
                        .append(", latest_load=").append(r.getLoadCurrentAmps());
            }
            sb.append("\n");
        }
        return openAiService.complete(
                "You are a fleet analytics assistant comparing electrical transformers for a maintenance manager.",
                sb.toString());
    }

    // ---------- helpers ----------

    private TransformerStatus deriveStatusFromHealth(double health) {
        if (health < 40) return TransformerStatus.CRITICAL;
        if (health < 70) return TransformerStatus.WARNING;
        return TransformerStatus.HEALTHY;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
