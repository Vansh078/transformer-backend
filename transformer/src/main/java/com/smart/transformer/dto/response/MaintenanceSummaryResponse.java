package com.smart.transformer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceSummaryResponse {
    private Long transformerId;
    private String transformerName;
    private int recordsConsidered;
    private String summary;
}
