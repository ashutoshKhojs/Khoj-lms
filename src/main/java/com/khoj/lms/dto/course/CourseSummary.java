package com.khoj.lms.dto.course;

import com.khoj.lms.enums.CourseStatus;
import com.khoj.lms.enums.DifficultyLevel;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseSummary {

    private UUID id;
    private String title;
    private String slug;
    private String shortDescription;
    private String thumbnailUrl;
    private DifficultyLevel difficultyLevel;
    private String language;
    private String categoryName;
    private String instructorName;
    private Boolean isFree;
    private BigDecimal price;
    private Integer enrolledCount;
    private Integer ratingCount;
    private BigDecimal averageRating;
    private Integer totalLessons;
    private Integer totalModules;
    private Long totalDurationSeconds;
    private CourseStatus status;
}