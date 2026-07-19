package com.smart.transformer.entity;

import com.smart.transformer.entity.enums.ReportStatus;
import com.smart.transformer.entity.enums.ReportType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Metadata row for a generated report (Report Management module).
 * Only the Supabase Storage object path is persisted here — never a signed URL,
 * since signed URLs expire and must be re-issued on demand (see ReportService#getDownloadUrl).
 */
@Getter
@Setter
@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_reports_transformer", columnList = "transformer_id, generated_at"),
        @Index(name = "idx_reports_type", columnList = "report_type, generated_at")
})
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null for fleet-wide reports; set for transformer-specific reports (manual/daily/critical). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformer_id")
    private Transformer transformer;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private ReportType reportType;

    @Column(name = "report_name", nullable = false, length = 255)
    private String reportName;

    /** Supabase Storage object path (e.g. "manual/TX-001-16921234.pdf") — NOT a signed URL. */
    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    /** User email/label who triggered generation, or "SYSTEM" for scheduled/critical reports. */
    @Column(name = "generated_by", length = 150)
    private String generatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;
}
