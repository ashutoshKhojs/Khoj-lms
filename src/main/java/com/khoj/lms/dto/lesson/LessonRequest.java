package com.khoj.lms.dto.lesson;

import com.khoj.lms.enums.LessonType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LessonRequest {

    @NotBlank(message = "Lesson title is required")
    @Size(max = 200)
    private String title;

    private String description;

    @NotNull(message = "Lesson type is required")
    private LessonType lessonType;

    private Integer displayOrder;

    // VIDEO
    private String videoS3Key;
    private Long videoDurationSeconds;
    private String videoThumbnailS3Key;

    // DOCUMENT
    private String notesS3Key;
    private String notesFileName;

    // TEXT
    private String textContent;

    private Boolean isPreview;
}