package com.khoj.lms.dto.category;

import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategorySummary {

    private UUID id;
    private String name;
    private String slug;
    private String iconUrl;
}