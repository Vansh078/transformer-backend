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
