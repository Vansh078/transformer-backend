package com.smart.transformer.service;

import com.smart.transformer.dto.response.SensorReadingResponse;
import com.smart.transformer.entity.Alert;
import com.smart.transformer.entity.Transformer;
import com.smart.transformer.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 2 AI features built on top of alerts:
 * - Explainable AI: a plain-English explanation of WHY an alert fired, grounded in
 *   the sensor readings/health score that triggered it.
 * - Automatic Incident Narratives: a short incident report a field engineer can read
 *   without digging through raw sensor data.
 *
 * Both are generated via OpenAI and cached on the Alert row so they're computed once.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertIntelligenceService {

    private final OpenAiService openAiService;
    private final AlertRepository alertRepository;
    private final SensorReadingService sensorReadingService;

    /**
     * Generates and persists the narrative + explanation for a freshly raised alert.
     * Runs after the alert transaction commits so a slow/failed OpenAI call never blocks
     * alert creation or email dispatch.
     */
    @Async
    @Transactional
    public void enrichAsync(Long alertId) {
        try {
            Alert alert = alertRepository.findById(alertId).orElse(null);
            if (alert == null) {
                return;
            }
            String grounding = buildGrounding(alert);
            alert.setNarrative(generateNarrative(alert, grounding));
            alert.setExplanation(generateExplanation(alert, grounding));
            alertRepository.save(alert);
        } catch (Exception e) {
            log.warn("Failed to enrich alert {} with AI narrative/explanation: {}", alertId, e.getMessage());
        }
    }

    /** Synchronous variant used by the on-demand "/explain" endpoint (regenerate). */
    @Transactional
    public Alert explainNow(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> com.smart.transformer.exception.ResourceNotFoundException.of("Alert", alertId));
        String grounding = buildGrounding(alert);
        alert.setNarrative(generateNarrative(alert, grounding));
        alert.setExplanation(generateExplanation(alert, grounding));
        return alertRepository.save(alert);
    }

    private String buildGrounding(Alert alert) {
        Transformer t = alert.getTransformer();
        SensorReadingResponse latest = sensorReadingService.getLatest(t.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("Transformer: ").append(t.getName()).append(" (").append(t.getAssetTag()).append(")\n");
        sb.append("Alert severity: ").append(alert.getSeverity()).append("\n");
        sb.append("Alert source: ").append(alert.getSource()).append("\n");
        sb.append("Alert message: ").append(alert.getMessage()).append("\n");
        sb.append("Transformer status: ").append(t.getStatus()).append("\n");
        sb.append("Health score: ").append(t.getHealthScore() != null ? t.getHealthScore() : "unknown").append("\n");

        if (latest != null) {
            sb.append("Latest sensor reading (").append(latest.getRecordedAt()).append("): ")
                    .append("temp=").append(latest.getTemperatureCelsius()).append("C, ")
                    .append("vibration=").append(latest.getVibrationMm()).append("mm, ")
                    .append("load=").append(latest.getLoadCurrentAmps()).append("A, ")
                    .append("voltage=").append(latest.getVoltageVolts()).append("V, ")
                    .append("anomaly_score=").append(latest.getAnomalyScore()).append("\n");
        } else {
            sb.append("No sensor readings available.\n");
        }
        return sb.toString();
    }

    private String generateNarrative(Alert alert, String grounding) {
        String system = "You write short incident narratives for electrical transformer maintenance teams. "
                + "Given the structured alert and sensor data below, write a 3-5 sentence plain-English "
                + "narrative describing what happened, when, and how severe it is. Do not invent data "
                + "that isn't present.\n\n" + grounding;
        return openAiService.complete(system, "Write the incident narrative now.");
    }

    private String generateExplanation(Alert alert, String grounding) {
        String system = "You are an explainable-AI assistant for a transformer anomaly detection system "
                + "(Isolation Forest based). Given the structured alert and sensor data below, explain in "
                + "2-4 bullet points WHICH readings most likely contributed to this alert and WHY, in terms "
                + "a field engineer (not a data scientist) can understand. Only reason from the data given.\n\n"
                + grounding;
        return openAiService.complete(system, "Explain why this alert fired.");
    }
}
