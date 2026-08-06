package com.uthman.VaultApi.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordReset(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("GoalForge — Reset your password");
        message.setText(
                "Hi,\n\n" +
                "We received a request to reset your GoalForge password.\n\n" +
                "Click the link below to choose a new password (valid for 1 hour):\n\n" +
                frontendUrl + "/reset-password?token=" + token + "\n\n" +
                "If you didn't request this, you can safely ignore this email.\n\n" +
                "— The GoalForge team"
        );
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }
}
