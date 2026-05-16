package com.khoj.lms.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String toName, String otp);
    void sendWelcomeEmail(String toEmail, String toName);
    void sendPasswordResetEmail(String toEmail, String toName, String resetToken);
    void sendInstructorApprovedEmail(String toEmail, String toName);
    void sendInstructorRejectedEmail(String toEmail, String toName, String reason);
    void sendCoursePublishedEmail(String toEmail, String instructorName, String courseTitle);
    void sendCourseEnrollmentEmail(
            String toEmail,
            String studentName,
            String courseTitle,
            String instructorName
    );
}