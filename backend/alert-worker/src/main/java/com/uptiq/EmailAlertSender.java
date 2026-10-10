package com.uptiq;

import com.uptiq.entity.AlertType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailAlertSender implements AlertSender {

    private final JavaMailSender mailSender;

    @Value("${alerts.mail.from}")
    private String fromAddress;

    public EmailAlertSender(
            JavaMailSender mailSender
    ) {
        this.mailSender = mailSender;
    }

    @Override
    public AlertType supports() {
        return AlertType.EMAIL;
    }

    @Override
    public void send(
            AlertChannelInfo channel,
            AlertPayload payload
    ) {

        String to = channel.config().get("email");

        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException(
                    "Email alert channel has no 'to' address"
            );
        }

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(fromAddress);
        message.setTo(to);

        message.setSubject(
                buildSubject(payload)
        );

        message.setText(
                buildBody(payload)
        );

        mailSender.send(message);
    }

    private String buildSubject(
            AlertPayload payload
    ) {

        return switch (payload.eventType()) {

            case DOWN ->
                    "🔴 [Uptiq] " +
                            payload.websiteName() +
                            " is DOWN";

            case RECOVERED ->
                    "🟢 [Uptiq] " +
                            payload.websiteName() +
                            " recovered";

            case SSL_EXPIRING ->
                    "⚠️ [Uptiq] SSL expiring in " +
                            payload.daysRemaining() +
                            " days — " +
                            payload.websiteName();
        };
    }

    private String buildBody(
            AlertPayload payload
    ) {

        return switch (payload.eventType()) {

            case DOWN ->
                    payload.websiteName() +
                            " is down.\n" +
                            (payload.reason() != null
                                    ? payload.reason() + "\n"
                                    : "") +
                            "URL: " +
                            payload.websiteUrl();

            case RECOVERED ->
                    payload.websiteName() +
                            " is back up.\nURL: " +
                            payload.websiteUrl();

            case SSL_EXPIRING ->
                    "SSL certificate for " +
                            payload.websiteName() +
                            " expires in " +
                            payload.daysRemaining() +
                            " days.\nURL: " +
                            payload.websiteUrl();
        };
    }
}