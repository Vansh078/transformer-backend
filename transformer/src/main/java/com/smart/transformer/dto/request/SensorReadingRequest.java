package com.smart.transformer.dto.request;
 
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
 
import java.time.Instant;
 
/**
 * Payload posted by ESP32 devices (or the FastAPI sidecar after enrichment).
 */
@Getter
@Setter
public class SensorReadingRequest {
 
    @NotNull(message = "Device UID is required")
    private String deviceUid;
 
    private Double temperatureCelsius;
    private Double vibrationMm;
    private Double loadCurrentAmps;
    private Double voltageVolts;
 
    private Instant recordedAt; // if null, server sets Instant.now()
}