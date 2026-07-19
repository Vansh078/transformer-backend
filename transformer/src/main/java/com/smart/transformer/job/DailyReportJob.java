package com.smart.transformer.job;

import com.smart.transformer.entity.Report;
import com.smart.transformer.entity.Transformer;
import com.smart.transformer.entity.enums.ReportStatus;
import com.smart.transformer.entity.enums.ReportType;
import com.smart.transformer.repository.TransformerRepository;
import com.smart.transformer.service.EmailService;
import com.smart.transformer.service.ReportService;
import com.smart.transformer.service.SupabaseStorageService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Quartz job that runs every day at midnight (see QuartzConfig): generates a daily
 * PDF health report for every transformer, uploads each to Supabase Storage,
 * saves its metadata, and emails the report link to maintenance engineers.
 *
 * NOTE: Quartz instantiates Job classes itself via a no-arg constructor (reflection),
 * bypassing normal Spring bean creation. Field-based @Autowired + the
 * AutowiringSpringBeanJobFactory in QuartzConfig is what makes injection work here —
 * constructor injection would NOT work for a Quartz-managed Job.
 */
@Slf4j
@Component
public class DailyReportJob implements Job {

    @Autowired
    private TransformerRepository transformerRepository;

    @Autowired
    private ReportService reportService;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @Autowired
    private EmailService emailService;

    @Value("${aws.ses.report-recipients:${aws.ses.alert-recipients}}")
    private String reportRecipients;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("Running scheduled daily report generation job");

        int succeeded = 0;
        int failed = 0;

        for (Transformer transformer : transformerRepository.findAll()) {
            try {
                Report report = reportService.generateScheduledReport(transformer, ReportType.DAILY);
                if (report.getStatus() == ReportStatus.GENERATED && report.getStoragePath() != null) {
                    String downloadUrl = supabaseStorageService.generateSignedUrl(report.getStoragePath(), 3600);
                    emailService.sendDailyReportEmail(reportRecipients, transformer, downloadUrl);
                    succeeded++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("Daily report generation failed for transformer {}: {}",
                        transformer.getAssetTag(), e.getMessage());
            }
        }

        log.info("Daily report generation job complete: {} succeeded, {} failed", succeeded, failed);
    }
}
