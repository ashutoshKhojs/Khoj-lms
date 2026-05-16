package com.khoj.lms.service.impl;

import com.khoj.lms.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Qualifier("emailTemplateEngine")
    private final SpringTemplateEngine templateEngine;

    // =========================================================
    // COMMON SEND METHOD
    // Stays private — called only from this class's public @Async methods.
    // Failures are logged loudly but NEVER rethrown, because rethrowing on a
    // background thread serves no purpose (no caller is listening).
    // =========================================================

    private void sendHtmlEmail(
            String toEmail,
            String subject,
            String templateName,
            Context context
    ) {

        try {

            String htmlContent =
                    templateEngine.process(
                            templateName,
                            context
                    );

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info(
                    "Email sent successfully to {} using template {} on thread {}",
                    toEmail,
                    templateName,
                    Thread.currentThread().getName()
            );

        } catch (Exception e) {

            // ❌ Do NOT rethrow — we are on a background thread.
            // ✅ Log loudly so it shows up in error log + monitoring.
            log.error(
                    "Failed to send email to {} (template={}): {}",
                    toEmail,
                    templateName,
                    e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // OTP EMAIL
    // =========================================================

    @Override
    @Async("emailTaskExecutor")
    public void sendOtpEmail(
            String toEmail,
            String toName,
            String otp
    ) {

        Context context = new Context();

        context.setVariable("name", toName);
        context.setVariable("otp", otp);
        context.setVariable("expiryMins", 5);

        sendHtmlEmail(
                toEmail,
                "OTP Verification",
                "otp-verification",
                context
        );
    }

    // =========================================================
    // WELCOME EMAIL
    // =========================================================

    @Override
    @Async("emailTaskExecutor")
    public void sendWelcomeEmail(
            String toEmail,
            String toName
    ) {

        Context context = new Context();

        context.setVariable("name", toName);

        sendHtmlEmail(
                toEmail,
                "Welcome to Khoj LMS",
                "welcome",
                context
        );
    }

    // =========================================================
    // PASSWORD RESET
    // =========================================================

    @Override
    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(
            String toEmail,
            String toName,
            String resetToken
    ) {

        Context context = new Context();

        context.setVariable("name", toName);
        context.setVariable("resetToken", resetToken);

        sendHtmlEmail(
                toEmail,
                "Password Reset",
                "password-reset",
                context
        );
    }

    // =========================================================
    // INSTRUCTOR APPROVED
    // =========================================================

    @Override
    @Async("emailTaskExecutor")
    public void sendInstructorApprovedEmail(
            String toEmail,
            String toName
    ) {

        Context context = new Context();

        context.setVariable("name", toName);

        sendHtmlEmail(
                toEmail,
                "Instructor Application Approved",
                "instructor-approved",
                context
        );
    }

    // =========================================================
    // INSTRUCTOR REJECTED
    // =========================================================

    @Override
    @Async("emailTaskExecutor")
    public void sendInstructorRejectedEmail(
            String toEmail,
            String toName,
            String reason
    ) {

        Context context = new Context();

        context.setVariable("name", toName);
        context.setVariable("reason", reason);

        sendHtmlEmail(
                toEmail,
                "Instructor Application Rejected",
                "instructor-rejected",
                context
        );
    }

    // =========================================================
    // COURSE PUBLISHED
    // =========================================================

    @Override
    @Async("emailTaskExecutor")
    public void sendCoursePublishedEmail(
            String toEmail,
            String instructorName,
            String courseTitle
    ) {

        Context context = new Context();

        context.setVariable(
                "instructorName",
                instructorName
        );

        context.setVariable(
                "courseTitle",
                courseTitle
        );

        sendHtmlEmail(
                toEmail,
                "Course Published Successfully",
                "course-published",
                context
        );
    }

    // =========================================================
    // COURSE ENROLLMENT
    // =========================================================

    @Override
    @Async("emailTaskExecutor")
    public void sendCourseEnrollmentEmail(
            String toEmail,
            String studentName,
            String courseTitle,
            String instructorName
    ) {

        Context context = new Context();

        context.setVariable("name", studentName);
        context.setVariable("courseTitle", courseTitle);
        context.setVariable("instructorName", instructorName);

        sendHtmlEmail(
                toEmail,
                "Course Enrollment Successful",
                "course-enrollment",
                context
        );
    }
}