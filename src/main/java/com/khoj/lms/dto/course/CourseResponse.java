package com.khoj.lms.dto.course;

import com.khoj.lms.dto.module.ModuleResponse;
import com.khoj.lms.enums.CourseStatus;
import com.khoj.lms.enums.DifficultyLevel;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponse {

    private UUID id;

    private String title;
    private String slug;

    private String shortDescription;
    private String description;

    private String whatYouWillLearn;
    private String prerequisites;
    private String targetAudience;

    private String thumbnailUrl;
    private String previewVideoUrl;

    private UUID categoryId;
    private String categoryName;

    private DifficultyLevel difficultyLevel;

    private String language;
    private String tags;

    private UUID instructorId;
    private String instructorName;
    private String instructorAvatar;

    private Boolean isFree;
    private BigDecimal price;

    private CourseStatus status;
    private LocalDateTime publishedAt;

    // Stats
    private Integer totalModules;
    private Integer totalLessons;
    private Long totalDurationSeconds;

    private Integer enrolledCount;

    private BigDecimal averageRating;
    private Integer ratingCount;

    private Boolean hasCertificate;
    private Integer certificateThreshold;

    // Nested modules
    private List<ModuleResponse> modules;
}