package com.khoj.lms.dto.module;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ModuleReorderRequest {

    @NotNull
    private UUID courseId;

    @NotNull
    private List<ModuleOrderItem> order;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ModuleOrderItem {
        private UUID moduleId;
        private Integer displayOrder;
    }
}