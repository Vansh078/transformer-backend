package com.smart.transformer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

/** Report Analytics — dashboard summary stats for the Reports section. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportStatsResponse {
    private long totalReports;
    private long reportsToday;
    private long criticalReports;
    private Instant lastGeneratedAt;
    private Map<String, Long> reportsByType;
}
