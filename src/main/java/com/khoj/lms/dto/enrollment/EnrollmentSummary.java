package com.khoj.lms.dto.enrollment;

import com.khoj.lms.enums.EnrollmentStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnrollmentSummary {

    private UUID    id;
    private UUID    courseId;
    private String  courseTitle;
    private String  courseSlug;
    private String  courseThumbnailUrl;
    private String  instructorName;

    private EnrollmentStatus status;
    private Double  progressPercentage;
    private Integer lessonsCompleted;
    private Integer totalLessons;

    private UUID    lastAccessedLessonId;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime enrolledAt;
}