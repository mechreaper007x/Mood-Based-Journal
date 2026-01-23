package com.example.moodjournal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Email service for sending password reset and other notification emails.
 * 
 * Uses JavaMailSender to send emails via SMTP (Resend).
 * Email functionality is optional - app will start without mail config.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${spring.mail.host:NOT_SET}")
    private String mailHost;

    @Value("${spring.mail.password:NOT_SET}")
    private String mailPassword;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    @PostConstruct
    public void init() {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║           EMAIL SERVICE CONFIGURATION                        ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ Email Enabled: {}", emailEnabled);
        log.info("║ Frontend URL: {}", frontendUrl);
        log.info("║ SMTP Host: {}", mailHost);
        log.info("║ SMTP Password: {}", mailPassword != null && mailPassword.length() > 5
                ? mailPassword.substring(0, 5) + "****"
                : "NOT_SET");
        log.info("║ JavaMailSender Available: {}", mailSenderProvider.getIfAvailable() != null);
        log.info("╚══════════════════════════════════════════════════════════════╝");
    }

    /**
     * Sends a password reset email to the user.
     * 
     * @param toEmail    The recipient's email address
     * @param resetToken The password reset token
     */
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        // Development mode: Always log to console first!
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║           PASSWORD RESET EMAIL (Dev Log)                     ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ To: {}", toEmail);
        log.info("║ Subject: Reset Your Mood Journal Password");
        log.info("║ Link: {}", resetLink);
        log.info("╚══════════════════════════════════════════════════════════════╝");

        if (emailEnabled) {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                log.warn("Email is enabled but JavaMailSender is not configured - skipping send");
                return;
            }
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                // IMPORTANT: Resend free tier ONLY allows sending from 'onboarding@resend.dev'
                // until you verify a custom domain.
                message.setFrom("onboarding@resend.dev");
                message.setTo(toEmail);
                message.setSubject("Reset Your Mood Journal Password");
                message.setText("Hello,\n\n"
                        + "You have requested to reset your password for Mood Journal.\n\n"
                        + "Click the link below to change your password:\n"
                        + resetLink + "\n\n"
                        + "This link will expire in 30 minutes.\n\n"
                        + "If you did not request this, please ignore this email.\n");

                mailSender.send(message);
                log.info("Password reset email sent successfully to {}", toEmail);
            } catch (Exception e) {
                log.error("╔══════════════════════════════════════════════════════════════╗");
                log.error("║           EMAIL SEND FAILED                                  ║");
                log.error("╠══════════════════════════════════════════════════════════════╣");
                log.error("║ To: {}", toEmail);
                log.error("║ Error: {}", e.getMessage());
                log.error("║ Cause: {}", e.getCause() != null ? e.getCause().getMessage() : "N/A");
                log.error("╚══════════════════════════════════════════════════════════════╝");
                log.error("Full stack trace:", e);
            }
        }
    }

    /**
     * Returns the password reset link for testing purposes.
     */
    public String getResetLink(String resetToken) {
        return frontendUrl + "/reset-password?token=" + resetToken;
    }
}
