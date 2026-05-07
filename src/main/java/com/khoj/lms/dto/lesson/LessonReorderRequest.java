package com.khoj.lms.dto.lesson;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LessonReorderRequest {

    @NotNull
    private UUID moduleId;

    @NotNull
    private List<LessonOrderItem> order;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class LessonOrderItem {
        private UUID lessonId;
        private Integer displayOrder;
    }
}