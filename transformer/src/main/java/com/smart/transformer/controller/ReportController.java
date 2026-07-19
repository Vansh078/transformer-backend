package com.smart.transformer.controller;

import com.smart.transformer.dto.response.ApiResponse;
import com.smart.transformer.dto.response.PagedResponse;
import com.smart.transformer.dto.response.ReportResponse;
import com.smart.transformer.dto.response.ReportStatsResponse;
import com.smart.transformer.entity.enums.ReportType;
import com.smart.transformer.service.ReportService;
import com.smart.transformer.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Report Management module. Role guidance (enforced at the service layer per
 * SecurityConfig's convention — @PreAuthorize left commented out while auth is
 * disabled for local testing, same as the rest of the codebase):
 *   - ADMIN: full access (generate, view, download, regenerate, delete/cleanup)
 *   - ENGINEER: generate and download reports
 *   - VIEWER: read-only access (view history, download)
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Generates a PDF health report for a transformer, stores it in Supabase Storage,
     * saves metadata in the database, and returns a signed download URL.
     */
    @PostMapping("/transformer/{transformerId}")
    //@PreAuthorize("hasRole('ADMIN') or hasRole('ENGINEER')")
    public ApiResponse<ReportResponse> generateReport(@PathVariable Long transformerId) {
        return ApiResponse.success("Report generated", reportService.generateManualReport(transformerId));
    }

    /** Regenerates an existing report (same transformer + report type) as a new report row. */
    @PostMapping("/{reportId}/regenerate")
    //@PreAuthorize("hasRole('ADMIN') or hasRole('ENGINEER')")
    public ApiResponse<ReportResponse> regenerate(@PathVariable Long reportId) {
        return ApiResponse.success("Report regenerated", reportService.regenerateReport(reportId));
    }

    /** Full report history with optional filters — transformer, report type, and date range. */
    @GetMapping
    public ApiResponse<PagedResponse<ReportResponse>> getHistory(
            @RequestParam(required = false) Long transformerId,
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageUtil.of(page, size, "generatedAt", "desc");
        return ApiResponse.success(reportService.getHistory(transformerId, reportType, from, to, pageable));
    }

    /** Report history for a specific transformer. */
    @GetMapping("/transformer/{transformerId}")
    public ApiResponse<PagedResponse<ReportResponse>> getByTransformer(
            @PathVariable Long transformerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageUtil.of(page, size, "generatedAt", "desc");
        return ApiResponse.success(reportService.getByTransformer(transformerId, pageable));
    }

    /** Most recently generated report — fleet-wide, or for a specific transformer. */
    @GetMapping("/latest")
    public ApiResponse<ReportResponse> getLatest(@RequestParam(required = false) Long transformerId) {
        return ApiResponse.success(reportService.getLatest(transformerId));
    }

    /** Issues a fresh signed download URL for an existing report (never persisted). */
    @GetMapping("/{reportId}/download")
    public ApiResponse<ReportResponse> download(@PathVariable Long reportId) {
        return ApiResponse.success(reportService.getDownloadUrl(reportId));
    }

    /** Report Analytics — totals, today's count, critical count, last generation time, by-type breakdown. */
    @GetMapping("/stats")
    public ApiResponse<ReportStatsResponse> getStats() {
        return ApiResponse.success(reportService.getStats());
    }
}
