package com.khoj.lms.dto.instructor;
import com.khoj.lms.enums.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

public class InstructorApplicationDtos {

    // ── Request ──────────────────────────────
    @Getter @Setter
    public static class ApplyRequest {

        @NotBlank(message = "Expertise is required")
        @Size(max = 500)
        private String expertise;

        @NotBlank(message = "Motivation is required")
        @Size(min = 100, max = 1000, message = "Motivation must be between 100 and 1000 characters")
        private String motivation;

        private String linkedinUrl;
        private String portfolioUrl;

        @Size(max = 500)
        private String experience;

        @Size(max = 500)
        private String qualifications;
    }

    @Getter @Setter
    public static class RejectRequest {
        @NotBlank(message = "Rejection reason is required")
        private String reason;
    }

    // ── Response ─────────────────────────────
    @Getter @Setter @Builder
    public static class ApplicationResponse {
        private UUID id;
        private UUID userId;
        private String applicantName;
        private String applicantEmail;
        private String expertise;
        private String motivation;
        private String linkedinUrl;
        private String portfolioUrl;
        private String experience;
        private String qualifications;
        private ApplicationStatus status;
        private String rejectionReason;
        private LocalDateTime reviewedAt;
        private LocalDateTime appliedAt;
    }
}
