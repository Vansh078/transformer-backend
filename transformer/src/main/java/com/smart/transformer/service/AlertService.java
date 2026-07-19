package com.smart.transformer.service;

import com.smart.transformer.dto.request.AlertRequest;
import com.smart.transformer.dto.response.AlertResponse;
import com.smart.transformer.entity.Alert;
import com.smart.transformer.entity.Transformer;
import com.smart.transformer.entity.enums.AlertSeverity;
import com.smart.transformer.exception.ResourceNotFoundException;
import com.smart.transformer.repository.AlertRepository;
import com.smart.transformer.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final TransformerService transformerService;
    private final EmailService emailService;

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

    public Page<AlertResponse> getOpenAlerts(Pageable pageable) {
        return alertRepository.findByAcknowledgedFalseOrderByCreatedAtDesc(pageable)
                .map(EntityMapper::toResponse);
    }

    public Page<AlertResponse> getByTransformer(Long transformerId, Pageable pageable) {
        return alertRepository.findByTransformerIdOrderByCreatedAtDesc(transformerId, pageable)
                .map(EntityMapper::toResponse);
    }

    public long countOpenCritical() {
        return alertRepository.countByAcknowledgedFalseAndSeverity(AlertSeverity.CRITICAL);
    }
}
