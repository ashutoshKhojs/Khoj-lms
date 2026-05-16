package com.khoj.lms.dto.course;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PublishedCourseUpdateRequest {

    @Size(max = 500)
    private String shortDescription;

    private String description;

    private String whatYouWillLearn;     // JSON array

    private String prerequisites;        // JSON array

    private String targetAudience;

    @Size(max = 500)
    private String thumbnailUrl;

    @Size(max = 500)
    private String previewVideoUrl;

    @Size(max = 500)
    private String tags;                 // comma-separated

    @DecimalMin("0.0")
    private BigDecimal price;
}