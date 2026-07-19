package com.smart.transformer.job;

import com.smart.transformer.entity.Transformer;
import com.smart.transformer.repository.TransformerRepository;
import com.smart.transformer.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Quartz job that emails a fleet health summary on a schedule
 * (wired up via QuartzConfig — daily/weekly/monthly cadence configurable there).
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
    private EmailService emailService;

    @Value("${aws.ses.alert-recipients}")
    private String recipients;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("Running scheduled fleet health summary job");

        StringBuilder html = new StringBuilder("<h2>Daily Fleet Health Summary</h2><table border='1' cellpadding='6'>");
        html.append("<tr><th>Asset Tag</th><th>Name</th><th>Status</th><th>Health Score</th></tr>");

        for (Transformer t : transformerRepository.findAll()) {
            html.append("<tr><td>").append(t.getAssetTag())
                    .append("</td><td>").append(t.getName())
                    .append("</td><td>").append(t.getStatus())
                    .append("</td><td>").append(t.getHealthScore() != null ? t.getHealthScore() : "-")
                    .append("</td></tr>");
        }
        html.append("</table>");

        emailService.sendReportEmail(recipients, "Daily Fleet Health Summary", html.toString());
    }
}
