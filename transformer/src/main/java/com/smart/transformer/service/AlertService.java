package com.smart.transformer.service;

import com.smart.transformer.dto.request.AlertRequest;
import com.smart.transformer.dto.response.AlertResponse;
import com.smart.transformer.entity.Alert;
import com.smart.transformer.entity.Transformer;
import com.smart.transformer.entity.enums.AlertSeverity;
import com.smart.transformer.event.AnomalyDetectedEvent;
import com.smart.transformer.exception.ResourceNotFoundException;
import com.smart.transformer.repository.AlertRepository;
import com.smart.transformer.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final TransformerService transformerService;
    private final EmailService emailService;
    private final AlertIntelligenceService alertIntelligenceService;
    private final ActivityLogService activityLogService;

    @Transactional
    public AlertResponse raise(AlertRequest request) {
        Transformer transformer = transformerService.getEntity(request.getTransformerId());

        Alert alert = new Alert();
        alert.setTransformer(transformer);
        alert.setSeverity(request.getSeverity());
        alert.setSource(request.getSource());
        alert.setMessage(request.getMessage());
        alert.setAcknowledged(false);

        Alert saved = alertRepository.save(alert);

        if (request.getSeverity() == AlertSeverity.CRITICAL) {
            emailService.sendCriticalAlertEmail(transformer, alert.getMessage());
        }

        // Phase 2 AI: auto-generate the incident narrative + explanation in the background
        alertIntelligenceService.enrichAsync(saved.getId());

        activityLogService.record("ALERT_RAISED", "Alert", saved.getId(),
                request.getSeverity() + " on transformer " + transformer.getAssetTag());

        return EntityMapper.toResponse(saved);
    }

    @Transactional
    public AlertResponse acknowledge(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> ResourceNotFoundException.of("Alert", alertId));
        alert.setAcknowledged(true);
        alert.setResolvedAt(Instant.now());
        return EntityMapper.toResponse(alertRepository.save(alert));
    }

    @Transactional(readOnly = true)
    public Page<AlertResponse> getOpenAlerts(Pageable pageable) {
        return alertRepository.findByAcknowledgedFalseOrderByCreatedAtDesc(pageable)
                .map(EntityMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AlertResponse> getByTransformer(Long transformerId, Pageable pageable) {
        return alertRepository.findByTransformerIdOrderByCreatedAtDesc(transformerId, pageable)
                .map(EntityMapper::toResponse);
    }

    public long countOpenCritical() {
        return alertRepository.countByAcknowledgedFalseAndSeverity(AlertSeverity.CRITICAL);
    }

    public AlertResponse explain(Long alertId) {
        return EntityMapper.toResponse(alertIntelligenceService.explainNow(alertId));
    }

    /**
     * Reacts to anomalies detected by HealthScoreService and raises an alert for them.
     * This is an event listener (rather than a direct method call from HealthScoreService)
     * specifically to avoid a circular bean dependency: AlertService already depends on
     * AlertIntelligenceService -> SensorReadingService -> HealthScoreService, so
     * HealthScoreService cannot also depend directly on AlertService.
     */
    @EventListener
    public void onAnomalyDetected(AnomalyDetectedEvent event) {
        try {
            AlertRequest alertRequest = new AlertRequest();
            alertRequest.setTransformerId(event.transformerId());
            alertRequest.setSeverity(event.severity());
            alertRequest.setSource(event.source());
            alertRequest.setMessage(event.toAlertMessage());
            raise(alertRequest);
        } catch (Exception e) {
            log.warn("Failed to raise alert from AnomalyDetectedEvent for transformer {}: {}",
                    event.transformerId(), e.getMessage());
        }
    }
}