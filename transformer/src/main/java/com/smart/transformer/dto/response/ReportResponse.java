package com.smart.transformer.dto.response;

import com.smart.transformer.entity.enums.ReportStatus;
import com.smart.transformer.entity.enums.ReportType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private Long id;
    private Long transformerId;
    private String transformerName;
    private ReportType reportType;
    private String reportName;
    private ReportStatus status;
    private Instant generatedAt;
    private String generatedBy;
    private Long fileSizeBytes;
    private String failureReason;

    /**
     * Freshly-issued short-lived signed URL — only populated on demand
     * (generate/download/regenerate responses), never persisted.
     */
    private String downloadUrl;
}
