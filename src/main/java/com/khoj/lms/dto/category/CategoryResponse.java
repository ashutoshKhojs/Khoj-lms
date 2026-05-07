package com.khoj.lms.dto.category;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String iconUrl;
    private Integer displayOrder;

    private UUID parentId;
    private String parentName;

    private Integer courseCount;

    private List<CategoryResponse> children;
}