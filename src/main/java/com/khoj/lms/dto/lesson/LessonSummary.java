package com.khoj.lms.dto.lesson;

import com.khoj.lms.enums.LessonType;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LessonSummary {

    private UUID id;
    private String title;
    private LessonType lessonType;
    private Integer displayOrder;
    private Long videoDurationSeconds;
    private Boolean isPreview;
    private Boolean isCompleted;
}