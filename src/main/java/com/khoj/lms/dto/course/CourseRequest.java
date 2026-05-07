package com.khoj.lms.dto.course;

import com.khoj.lms.enums.DifficultyLevel;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseRequest {

    @NotBlank
    @Size(min = 5, max = 200)
    private String title;

    @Size(max = 500)
    private String shortDescription;

    private String description;
    private String whatYouWillLearn;
    private String prerequisites;
    private String targetAudience;

    private String thumbnailUrl;
    private String previewVideoUrl;

    private UUID categoryId;

    @NotNull
    private DifficultyLevel difficultyLevel;

    private String language;
    private String tags;

    private Boolean isFree;

    @DecimalMin("0.0")
    private BigDecimal price;

    private Boolean hasCertificate;

    @Min(0) @Max(100)
    private Integer certificateThreshold;
}