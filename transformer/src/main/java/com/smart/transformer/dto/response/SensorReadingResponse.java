package com.smart.transformer.dto.response;
 
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
import java.time.Instant;
 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SensorReadingResponse {
    private Long id;
    private Long transformerId;
    private String deviceUid;
    private Double temperatureCelsius;
    private Double vibrationMm;
    private Double loadCurrentAmps;
    private Double voltageVolts;
    private Double anomalyScore;
    private Instant recordedAt;
}