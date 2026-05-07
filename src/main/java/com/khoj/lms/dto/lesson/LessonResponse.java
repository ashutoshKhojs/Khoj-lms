package com.khoj.lms.dto.lesson;

import com.khoj.lms.enums.LessonType;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LessonResponse {

    private UUID id;
    private UUID moduleId;
    private UUID courseId;
    private String title;
    private String description;
    private LessonType lessonType;
    private Integer displayOrder;
    private Boolean isPreview;
    private Boolean isPublished;

    // VIDEO
    private String videoUrl;
    private Long videoDurationSeconds;
    private String videoThumbnailUrl;

    // DOCUMENT
    private String notesUrl;
    private String notesFileName;

    // TEXT
    private String textContent;

    // Navigation
    private UUID previousLessonId;
    private UUID nextLessonId;
}