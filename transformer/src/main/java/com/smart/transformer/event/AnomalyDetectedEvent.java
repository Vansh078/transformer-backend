package com.smart.transformer.event;

import com.smart.transformer.entity.enums.AlertSeverity;
import com.smart.transformer.entity.enums.AlertSource;

/**
 * Published by {@code HealthScoreService} whenever a sensor reading is scored as
 * anomalous enough to warrant an alert. Listeners (e.g. {@code AlertService}) react
 * to this instead of being called directly, which is what breaks the circular
 * dependency: HealthScoreService -> AlertService -> AlertIntelligenceService ->
 * SensorReadingService -> HealthScoreService.
 */
public record AnomalyDetectedEvent(
        Long transformerId,
        AlertSeverity severity,
        AlertSource source,
        double anomalyScore,
        double healthScore
) {

    public String toAlertMessage() {
        return "Anomaly detected — score " + anomalyScore + ", predicted health score " + healthScore;
    }
}
