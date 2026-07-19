package com.smart.transformer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FailureCostResponse {
    private Long transformerId;
    private String transformerName;
    private Double failureProbability;       // 0..1, derived from current health score
    private Double totalFailureCost;         // replacementCost + downtime cost
    private Double expectedCostOfFailure;    // totalFailureCost * failureProbability
    private Double preventiveMaintenanceCost;
    private Double netSavingsFromPrevention; // expectedCostOfFailure - preventiveMaintenanceCost
    private String recommendation;
}
