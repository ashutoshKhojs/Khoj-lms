package com.khoj.lms.dto.enrollment;

import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseEnrollmentStats {
    private UUID    courseId;
    private String  courseTitle;
    private long    totalEnrollments;
    private long    activeEnrollments;
    private long    completedEnrollments;
    private double  averageProgress;        // 0–100
    private double  completionRate;         // 0–100
}