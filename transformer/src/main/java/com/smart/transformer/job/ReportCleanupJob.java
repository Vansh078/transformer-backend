package com.smart.transformer.job;

import com.smart.transformer.entity.Report;
import com.smart.transformer.entity.enums.ReportStatus;
import com.smart.transformer.repository.ReportRepository;
import com.smart.transformer.service.SupabaseStorageService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Cleanup & Maintenance job (Report Management module):
 *  - Signed URLs are never persisted, so there is nothing to "expire" in the DB —
 *    a fresh one is minted on every download request (see ReportService#getDownloadUrl).
 *  - This job instead archives/deletes reports whose age exceeds the configured
 *    retention period, removing the underlying Supabase object and marking the
 *    metadata row ARCHIVED (kept for audit history rather than hard-deleted).
 *
 * Runs weekly (see QuartzConfig) — retention window is configurable via
 * `reports.retention-days` (default 90).
 */
@Slf4j
@Component
public class ReportCleanupJob implements Job {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @Value("${reports.retention-days:90}")
    private int retentionDays;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        List<Report> expired = reportRepository.findByStatusAndGeneratedAtBefore(ReportStatus.GENERATED, cutoff);

        log.info("Running report retention cleanup: {} report(s) older than {} days", expired.size(), retentionDays);

        for (Report report : expired) {
            try {
                if (report.getStoragePath() != null) {
                    supabaseStorageService.deleteObject(report.getStoragePath());
                }
                report.setStatus(ReportStatus.ARCHIVED);
                reportRepository.save(report);
            } catch (Exception e) {
                log.warn("Failed to archive/delete report {}: {}", report.getId(), e.getMessage());
            }
        }
    }
}
