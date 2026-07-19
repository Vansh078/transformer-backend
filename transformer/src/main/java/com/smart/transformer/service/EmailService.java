package com.smart.transformer.service;

import com.smart.transformer.entity.Transformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SesClient sesClient;

    @Value("${aws.ses.from-address}")
    private String fromAddress;

    @Value("${aws.ses.alert-recipients}")
    private String alertRecipients; // comma-separated

    public void sendCriticalAlertEmail(Transformer transformer, String message) {
        send(alertRecipients,
                "CRITICAL Alert: " + transformer.getName() + " (" + transformer.getAssetTag() + ")",
                buildAlertHtml(transformer, message));
    }

    public void sendReportEmail(String toAddress, String subject, String htmlBody) {
        send(toAddress, subject, htmlBody);
    }

    /**
     * Sends a link to a freshly generated daily report for a single transformer.
     * Used by the daily scheduled report job (Report Management module).
     */
    public void sendDailyReportEmail(String toAddresses, Transformer transformer, String downloadUrl) {
        send(toAddresses,
                "Daily Health Report: " + transformer.getName() + " (" + transformer.getAssetTag() + ")",
                buildReportHtml(transformer, "Daily Health Report", downloadUrl,
                        "Your scheduled daily health report is ready.", BRAND_ACCENT));
    }

    /**
     * Sends an immediate notification with a link to a critical event report.
     * Used when a transformer enters a critical state (health/temperature/voltage/alerts).
     */
    public void sendCriticalReportEmail(String toAddresses, Transformer transformer, String downloadUrl) {
        send(toAddresses,
                "CRITICAL Report: " + transformer.getName() + " (" + transformer.getAssetTag() + ")",
                buildReportHtml(transformer, "Critical Health Report", downloadUrl,
                        "This transformer has entered a critical state. A detailed report has been generated.",
                        CRITICAL_ACCENT));
    }

    private static final String BRAND_ACCENT = "#1e40af";
    private static final String CRITICAL_ACCENT = "#c0392b";

    private String buildReportHtml(Transformer transformer, String reportTitle, String downloadUrl,
                                    String intro, String accentColor) {
        return """
                <html>
                  <body style="font-family: Arial, sans-serif;">
                    <h2 style="color:%s;">%s</h2>
                    <p>%s</p>
                    <p><strong>Transformer:</strong> %s (%s)</p>
                    <p><strong>Location:</strong> %s</p>
                    <p><a href="%s" style="background:%s;color:#fff;padding:10px 16px;text-decoration:none;border-radius:4px;">Download Report</a></p>
                    <p style="font-size:12px;color:#888;">This download link expires in 1 hour. Request a new link from the Reports dashboard if it lapses.</p>
                  </body>
                </html>
                """.formatted(
                accentColor, reportTitle, intro,
                transformer.getName(), transformer.getAssetTag(),
                transformer.getLocation() != null ? transformer.getLocation() : "N/A",
                downloadUrl != null ? downloadUrl : "#",
                accentColor
        );
    }

    private void send(String toAddresses, String subject, String htmlBody) {
        try {
            Destination destination = Destination.builder()
                    .toAddresses(toAddresses.split(","))
                    .build();

            Content subjectContent = Content.builder().data(subject).charset("UTF-8").build();
            Content bodyContent = Content.builder().data(htmlBody).charset("UTF-8").build();
            Body body = Body.builder().html(bodyContent).build();
            Message message = Message.builder().subject(subjectContent).body(body).build();

            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromAddress)
                    .destination(destination)
                    .message(message)
                    .build();

            sesClient.sendEmail(request);
        } catch (SesException e) {
            log.error("Failed to send email via SES: {}", e.awsErrorDetails() != null
                    ? e.awsErrorDetails().errorMessage() : e.getMessage());
        }
    }

    private String buildAlertHtml(Transformer transformer, String message) {
        return """
                <html>
                  <body style="font-family: Arial, sans-serif;">
                    <h2 style="color:#c0392b;">Critical Transformer Alert</h2>
                    <p><strong>Transformer:</strong> %s (%s)</p>
                    <p><strong>Location:</strong> %s</p>
                    <p><strong>Details:</strong> %s</p>
                  </body>
                </html>
                """.formatted(
                transformer.getName(),
                transformer.getAssetTag(),
                transformer.getLocation() != null ? transformer.getLocation() : "N/A",
                message
        );
    }
}
