package com.khoj.lms.dto.progress;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LessonProgressResponse {

    private UUID id;
    private UUID lessonId;
    private String lessonTitle;
    private UUID courseId;
    private UUID enrollmentId;

    private Boolean isCompleted;
    private LocalDateTime completedAt;

    // Video
    private Long   watchPositionSeconds;
    private Long   totalWatchTimeSeconds;
    private Double watchPercentage;

    // Access
    private LocalDateTime firstAccessedAt;
    private LocalDateTime lastAccessedAt;
    private Integer accessCount;
}