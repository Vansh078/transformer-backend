package com.smart.transformer.service;

import com.smart.transformer.dto.response.PagedResponse;
import com.smart.transformer.dto.response.ReportResponse;
import com.smart.transformer.dto.response.ReportStatsResponse;
import com.smart.transformer.dto.response.SensorReadingResponse;
import com.smart.transformer.entity.Alert;
import com.smart.transformer.entity.MaintenanceRecord;
import com.smart.transformer.entity.Report;
import com.smart.transformer.entity.Transformer;
import com.smart.transformer.entity.enums.ReportStatus;
import com.smart.transformer.entity.enums.ReportType;
import com.smart.transformer.exception.ResourceNotFoundException;
import com.smart.transformer.repository.AlertRepository;
import com.smart.transformer.repository.MaintenanceRecordRepository;
import com.smart.transformer.repository.ReportRepository;
import com.smart.transformer.repository.spec.ReportSpecifications;
import com.smart.transformer.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Central orchestrator for the Report Management module. Handles manual, scheduled
 * (daily/weekly/monthly), and critical-event report generation:
 *   1. Gathers transformer/sensor/alert/maintenance data
 *   2. Builds the PDF (PdfReportService)
 *   3. Uploads it to a report-type folder in Supabase Storage (SupabaseStorageService)
 *   4. Persists metadata — storage path only, never a signed URL (ReportRepository)
 *   5. Issues a short-lived signed URL on demand for the caller/email
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int SIGNED_URL_TTL_SECONDS = 3600; // 1 hour

    private final ReportRepository reportRepository;
    private final TransformerService transformerService;
    private final SensorReadingService sensorReadingService;
    private final AlertRepository alertRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final PdfReportService pdfReportService;
    private final SupabaseStorageService supabaseStorageService;
    private final ActivityLogService activityLogService;

    // ---------- Manual generation (dashboard "Generate Report" button) ----------

    @Transactional
    public ReportResponse generateManualReport(Long transformerId) {
        Report report = generateAndStore(transformerId, ReportType.MANUAL, currentActorLabel());
        activityLogService.record("REPORT_GENERATED", "Transformer", transformerId,
                "Manual report generated: " + report.getReportName());
        return toResponseWithSignedUrl(report);
    }

    // ---------- Scheduled generation (daily/weekly/monthly jobs) ----------

    @Transactional
    public Report generateScheduledReport(Transformer transformer, ReportType reportType) {
        return generateAndStore(transformer.getId(), reportType, "SYSTEM");
    }

    // ---------- Critical event generation ----------

    @Transactional
    public Report generateCriticalReport(Transformer transformer) {
        Report report = generateAndStore(transformer.getId(), ReportType.CRITICAL, "SYSTEM");
        activityLogService.record("CRITICAL_REPORT_GENERATED", "Transformer", transformer.getId(),
                "Critical report generated: " + report.getReportName());
        return report;
    }

    // ---------- Regeneration ----------

    @Transactional
    public ReportResponse regenerateReport(Long reportId) {
        Report existing = reportRepository.findById(reportId)
                .orElseThrow(() -> ResourceNotFoundException.of("Report", reportId));

        Long transformerId = existing.getTransformer() != null ? existing.getTransformer().getId() : null;
        if (transformerId == null) {
            throw new ResourceNotFoundException("Report " + reportId + " has no associated transformer to regenerate against");
        }

        Report regenerated = generateAndStore(transformerId, existing.getReportType(), currentActorLabel());
        activityLogService.record("REPORT_REGENERATED", "Report", reportId,
                "Regenerated as report " + regenerated.getId());
        return toResponseWithSignedUrl(regenerated);
    }

    // ---------- History / retrieval ----------

    @Transactional(readOnly = true)
    public PagedResponse<ReportResponse> getHistory(Long transformerId, ReportType reportType,
                                                      Instant from, Instant to, Pageable pageable) {
        Page<Report> page = reportRepository.findAll(
                ReportSpecifications.withFilters(transformerId, reportType, from, to), pageable);
        return PagedResponse.from(page.map(EntityMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReportResponse> getByTransformer(Long transformerId, Pageable pageable) {
        Page<Report> page = reportRepository.findByTransformerIdOrderByGeneratedAtDesc(transformerId, pageable);
        return PagedResponse.from(page.map(EntityMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ReportResponse getLatest(Long transformerId) {
        Report latest = transformerId != null
                ? reportRepository.findFirstByTransformerIdOrderByGeneratedAtDesc(transformerId)
                : reportRepository.findFirstByOrderByGeneratedAtDesc();
        return latest != null ? EntityMapper.toResponse(latest) : null;
    }

    /**
     * Returns a fresh signed download URL for an existing report. Signed URLs are never
     * persisted — a new one is minted on every download request against the stored object path.
     */
    @Transactional(readOnly = true)
    public ReportResponse getDownloadUrl(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> ResourceNotFoundException.of("Report", reportId));
        if (report.getStatus() != ReportStatus.GENERATED) {
            throw new IllegalStateException("Report " + reportId + " is not in a downloadable state: " + report.getStatus());
        }
        return toResponseWithSignedUrl(report);
    }

    // ---------- Analytics ----------

    @Transactional(readOnly = true)
    public ReportStatsResponse getStats() {
        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfToday = LocalDate.now(ZoneOffset.UTC).atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);

        long total = reportRepository.count();
        long today = reportRepository.countByGeneratedAtBetween(startOfToday, endOfToday);
        long critical = reportRepository.countByReportType(ReportType.CRITICAL);
        Report latest = reportRepository.findFirstByOrderByGeneratedAtDesc();

        java.util.Map<String, Long> byType = new java.util.LinkedHashMap<>();
        for (ReportType type : ReportType.values()) {
            byType.put(type.name(), reportRepository.countByReportType(type));
        }

        return new ReportStatsResponse(total, today, critical,
                latest != null ? latest.getGeneratedAt() : null, byType);
    }

    // ---------- shared generation pipeline ----------

    private Report generateAndStore(Long transformerId, ReportType reportType, String generatedBy) {
        Transformer transformer = transformerService.getEntity(transformerId);

        Report report = new Report();
        report.setTransformer(transformer);
        report.setReportType(reportType);
        report.setGeneratedBy(generatedBy);
        report.setStatus(ReportStatus.PENDING);
        report.setGeneratedAt(Instant.now());
        report.setReportName(buildReportName(transformer, reportType));
        report = reportRepository.save(report);

        try {
            List<SensorReadingResponse> readings = sensorReadingService
                    .getHistory(transformerId, org.springframework.data.domain.PageRequest.of(0, 50))
                    .getContent();
            List<Alert> alerts = alertRepository.findTop50ByTransformerIdOrderByCreatedAtDesc(transformerId);
            List<MaintenanceRecord> maintenanceRecords = maintenanceRecordRepository
                    .findByTransformerIdOrderByPerformedAtDesc(transformerId);

            byte[] pdfBytes = pdfReportService.buildTransformerReport(
                    transformer, readings, alerts, maintenanceRecords, reportTitle(reportType));

            String fileName = transformer.getAssetTag() + "-" + Instant.now().toEpochMilli() + ".pdf";
            String objectPath = supabaseStorageService.uploadPdf(folderFor(reportType), fileName, pdfBytes);

            report.setStoragePath(objectPath);
            report.setFileSizeBytes((long) pdfBytes.length);
            report.setStatus(ReportStatus.GENERATED);
            return reportRepository.save(report);
        } catch (Exception e) {
            log.error("Failed to generate {} report for transformer {}: {}", reportType, transformerId, e.getMessage());
            report.setStatus(ReportStatus.FAILED);
            report.setFailureReason(e.getMessage());
            return reportRepository.save(report);
        }
    }

    private ReportResponse toResponseWithSignedUrl(Report report) {
        ReportResponse response = EntityMapper.toResponse(report);
        if (report.getStatus() == ReportStatus.GENERATED && report.getStoragePath() != null) {
            response.setDownloadUrl(supabaseStorageService.generateSignedUrl(report.getStoragePath(), SIGNED_URL_TTL_SECONDS));
        }
        return response;
    }

    private String folderFor(ReportType reportType) {
        return reportType.name().toLowerCase();
    }

    private String reportTitle(ReportType reportType) {
        return switch (reportType) {
            case MANUAL -> "Manual Health Report";
            case DAILY -> "Daily Health Report";
            case WEEKLY -> "Weekly Health Report";
            case MONTHLY -> "Monthly Health Report";
            case CRITICAL -> "Critical Health Report";
        };
    }

    private String buildReportName(Transformer transformer, ReportType reportType) {
        return transformer.getAssetTag() + " - " + reportTitle(reportType) + " - "
                + LocalDate.now(ZoneOffset.UTC);
    }

    private String currentActorLabel() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                Object email = jwt.getClaims().get("email");
                return email != null ? email.toString() : jwt.getSubject();
            }
        } catch (Exception ignored) {
        }
        return "system";
    }
}
