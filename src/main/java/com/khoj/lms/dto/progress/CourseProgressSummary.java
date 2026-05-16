package com.khoj.lms.dto.progress;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseProgressSummary {

    private UUID    courseId;
    private UUID    enrollmentId;
    private Integer totalLessons;
    private Integer lessonsCompleted;
    private Double  progressPercentage;
    private Long    totalWatchTimeSeconds;
    private List<LessonProgressResponse> lessonProgress;
}