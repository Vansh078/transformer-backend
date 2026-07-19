package com.smart.transformer.service;

import com.smart.transformer.entity.SensorReading;
import com.smart.transformer.entity.Transformer;
import com.smart.transformer.entity.enums.AlertSeverity;
import com.smart.transformer.entity.enums.AlertSource;
import com.smart.transformer.entity.enums.TransformerStatus;
import com.smart.transformer.event.AnomalyDetectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Talks to the FastAPI sidecar (Isolation Forest) to score a sensor reading
 * for anomalies and derive a rolling health score for the transformer.
 *
 * NOTE: this service intentionally does NOT depend on AlertService. Raising an
 * alert here used to be a direct method call, but AlertService depends
 * (transitively, via AlertIntelligenceService -> SensorReadingService) on
 * HealthScoreService, which created a circular bean dependency. Instead, this
 * service publishes an {@link AnomalyDetectedEvent} and lets whoever cares
 * (AlertService) listen for it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthScoreService {

    private final WebClient aiSidecarWebClient;
    private final TransformerService transformerService;
    private final ApplicationEventPublisher eventPublisher;

    public void scoreAsync(SensorReading reading) {
        Transformer transformer = reading.getTransformer();

        Map<String, Object> payload = Map.of(
                "transformer_id", transformer.getId(),
                "temperature_celsius", nvl(reading.getTemperatureCelsius()),
                "oil_level_percent", nvl(reading.getOilLevelPercent()),
                "vibration_mm", nvl(reading.getVibrationMm()),
                "load_current_amps", nvl(reading.getLoadCurrentAmps()),
                "voltage_volts", nvl(reading.getVoltageVolts()),
                "humidity_percent", nvl(reading.getHumidityPercent())
        );

        aiSidecarWebClient.post()
                .uri("/api/v1/score")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(ScoreResult.class)
                .doOnError(err -> log.warn("AI sidecar scoring failed for transformer {}: {}",
                        transformer.getId(), err.getMessage()))
                .doOnSuccess(result -> {
                    if (result != null) {
                        applyScore(transformer.getId(), result);
                    }
                })
                .subscribe(); // fire-and-forget so ingestion never blocks on the AI service
    }

    private void applyScore(Long transformerId, ScoreResult result) {
        TransformerStatus status = deriveStatus(result.anomalyScore());
        transformerService.updateHealthScore(transformerId, result.healthScore(), status);

        if (status == TransformerStatus.CRITICAL || status == TransformerStatus.WARNING) {
            eventPublisher.publishEvent(new AnomalyDetectedEvent(
                    transformerId,
                    status == TransformerStatus.CRITICAL ? AlertSeverity.CRITICAL : AlertSeverity.WARNING,
                    AlertSource.AI_ANOMALY_DETECTION,
                    result.anomalyScore(),
                    result.healthScore()
            ));
        }
    }

    private TransformerStatus deriveStatus(double anomalyScore) {
        if (anomalyScore >= 0.8) return TransformerStatus.CRITICAL;
        if (anomalyScore >= 0.5) return TransformerStatus.WARNING;
        return TransformerStatus.HEALTHY;
    }

    private double nvl(Double value) {
        return value != null ? value : 0.0;
    }

    // FastAPI/Python will send snake_case JSON (e.g. "anomaly_score"); map it explicitly
    // since Jackson does not convert snake_case -> camelCase by default.
    public record ScoreResult(
            @com.fasterxml.jackson.annotation.JsonProperty("anomaly_score") double anomalyScore,
            @com.fasterxml.jackson.annotation.JsonProperty("health_score") double healthScore) {}
}
