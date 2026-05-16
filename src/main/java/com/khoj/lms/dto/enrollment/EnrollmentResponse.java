package com.khoj.lms.dto.enrollment;

import com.khoj.lms.enums.EnrollmentStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnrollmentResponse {

    private UUID id;

    // Course snapshot
    private UUID   courseId;
    private String courseSlug;
    private String courseTitle;
    private String courseThumbnailUrl;
    private String instructorName;
    private Integer totalLessons;
    private Long    totalDurationSeconds;

    // Student snapshot
    private UUID   studentId;
    private String studentName;
    private String studentEmail;

    // Status
    private EnrollmentStatus status;

    // Progress
    private Double  progressPercentage;
    private Integer lessonsCompleted;
    private Long    totalTimeSpentSeconds;

    // Resume
    private UUID   lastAccessedLessonId;
    private String lastAccessedLessonTitle;
    private LocalDateTime lastAccessedAt;

    // Completion
    private LocalDateTime completedAt;
    private Boolean isCertificateIssued;

    // Review
    private Integer rating;
    private String  review;
    private LocalDateTime reviewedAt;

    // Audit
    private LocalDateTime enrolledAt;
}