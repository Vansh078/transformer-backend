package com.smart.transformer.dto.response;

import com.smart.transformer.entity.enums.TransformerStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoadSimulationResponse {
    private Long transformerId;
    private String transformerName;

    private Double baselineLoadAmps;
    private Double simulatedLoadAmps;

    private Double baselineTemperatureCelsius;
    private Double simulatedTemperatureCelsius;

    private Double baselineHealthScore;
    private Double simulatedHealthScore;

    private TransformerStatus baselineStatus;
    private TransformerStatus simulatedStatus;

    private String narrative; // OpenAI-generated plain-English risk explanation
}
