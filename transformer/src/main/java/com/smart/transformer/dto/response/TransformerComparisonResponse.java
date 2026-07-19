package com.smart.transformer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Phase 3 "Transformer Comparison" — side-by-side metrics plus an AI-written verdict. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransformerComparisonResponse {
    private List<TransformerResponse> transformers;
    private List<SensorReadingResponse> latestReadings; // same order as transformers, nulls allowed
    private List<Long> maintenanceRecordCounts;          // same order as transformers
    private List<Long> openAlertCounts;                  // same order as transformers
    private String aiVerdict; // OpenAI summary of which is healthiest / needs attention
}
