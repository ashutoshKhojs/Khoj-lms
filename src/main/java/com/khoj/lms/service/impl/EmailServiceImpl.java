package com.khoj.lms.service.impl;

import com.khoj.lms.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Value("${app.mail.base-url}")
    private String baseUrl;

    // ─────────────────────────────────────────
    // OTP Email
    // ─────────────────────────────────────────

    @Async
    @Override
    public void sendOtpEmail(String toEmail, String toName, String otp) {
        sendEmail(
                toEmail,
                toName,
                "Verify your Khoj account — OTP inside",
                "otp-verification",
                Map.of(
                        "name",         firstName(toName),
                        "otp",          otp,
                        "expiryMins",   "15",
                        "year",         String.valueOf(java.time.Year.now().getValue())
                )
        );
    }

    // ─────────────────────────────────────────
    // Welcome Email
    // ─────────────────────────────────────────

    @Async
    @Override
    public void sendWelcomeEmail(String toEmail, String toName) {
        sendEmail(
                toEmail,
                toName,
                "Welcome to Khoj — Your learning journey starts now! 🚀",
                "welcome",
                Map.of(
                        "name",     firstName(toName),
                        "baseUrl",  baseUrl,
                        "year",     String.valueOf(java.time.Year.now().getValue())
                )
        );
    }

    // ─────────────────────────────────────────
    // Password Reset Email
    // ─────────────────────────────────────────

    @Async
    @Override
    public void sendPasswordResetEmail(String toEmail, String toName, String resetToken) {
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;
        sendEmail(
                toEmail,
                toName,
                "Reset your Khoj password",
                "password-reset",
                Map.of(
                        "name",       firstName(toName),
                        "resetLink",  resetLink,
                        "expiryMins", "30",
                        "year",       String.valueOf(java.time.Year.now().getValue())
                )
        );
    }

    // ─────────────────────────────────────────
    // Instructor Approved
    // ─────────────────────────────────────────

    @Async
    @Override
    public void sendInstructorApprovedEmail(String toEmail, String toName) {
        sendEmail(
                toEmail,
                toName,
                "Congratulations! You're now a Khoj Instructor 🎉",
                "instructor-approved",
                Map.of(
                        "name",     firstName(toName),
                        "baseUrl",  baseUrl,
                        "year",     String.valueOf(java.time.Year.now().getValue())
                )
        );
    }

    // ─────────────────────────────────────────
    // Instructor Rejected
    // ─────────────────────────────────────────

    @Async
    @Override
    public void sendInstructorRejectedEmail(String toEmail, String toName, String reason) {
        sendEmail(
                toEmail,
                toName,
                "Update on your Khoj Instructor Application",
                "instructor-rejected",
                Map.of(
                        "name",     firstName(toName),
                        "reason",   reason,
                        "baseUrl",  baseUrl,
                        "year",     String.valueOf(java.time.Year.now().getValue())
                )
        );
    }

    // ─────────────────────────────────────────
    // Course Published
    // ─────────────────────────────────────────

    @Async
    @Override
    public void sendCoursePublishedEmail(String toEmail, String instructorName, String courseTitle) {
        sendEmail(
                toEmail,
                instructorName,
                "Your course is LIVE on Khoj! 🚀",
                "course-published",
                Map.of(
                        "name",         firstName(instructorName),
                        "courseTitle",  courseTitle,
                        "baseUrl",      baseUrl,
                        "year",         String.valueOf(java.time.Year.now().getValue())
                )
        );
    }

    // ─────────────────────────────────────────
    // Core send method
    // ─────────────────────────────────────────

    private void sendEmail(String toEmail, String toName, String subject,
                           String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);
            log.info("Email sent → [{}] to {}", templateName, toEmail);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email [{}] to {}: {}", templateName, toEmail, e.getMessage());
            // Don't throw — email failure should never break the main flow
        }
    }

    private String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "there";
        return fullName.split(" ")[0];
    }
}