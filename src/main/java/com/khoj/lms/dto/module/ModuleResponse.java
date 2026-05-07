package com.khoj.lms.dto.module;

import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ModuleResponse {

    private UUID id;
    private UUID courseId;
    private String title;
    private String description;
    private Integer displayOrder;
    private Boolean isPublished;
    private Boolean isLocked;
    private Integer totalLessons;
    private Long totalDurationSeconds;
}