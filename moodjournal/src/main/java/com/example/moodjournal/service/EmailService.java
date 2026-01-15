package com.example.moodjournal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Email service for sending password reset and other notification emails.
 * 
 * Uses JavaMailSender to send emails via SMTP (Resend).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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
                log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
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
