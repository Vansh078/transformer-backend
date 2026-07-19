package com.smart.transformer.dto.response;

import com.smart.transformer.entity.enums.TransformerStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A single point on the Phase 3 "Fleet Risk Heatmap". */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FleetRiskPointResponse {
    private Long transformerId;
    private String assetTag;
    private String name;
    private String location;
    private Double latitude;
    private Double longitude;
    private TransformerStatus status;
    private Double healthScore;
    private Double riskScore;      // 0..100, higher = riskier (derived from health score + open alerts)
    private long openCriticalAlerts;
    private long openWarningAlerts;
}
