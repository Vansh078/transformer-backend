package com.smart.transformer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "sensor_readings", indexes = {
        @Index(name = "idx_reading_transformer_time", columnList = "transformer_id, recorded_at")
})
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transformer_id", nullable = false)
    private Transformer transformer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    private Double temperatureCelsius;

    private Double oilLevelPercent;

    private Double vibrationMm;

    private Double loadCurrentAmps;

    private Double voltageVolts;

    private Double humidityPercent;

    @Column(name = "anomaly_score")
    private Double anomalyScore; // set by AI sidecar

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}
