package com.khoj.lms.dto.course;

import com.khoj.lms.enums.CourseStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InstructorCourseView {

    private UUID id;
    private String title;
    private String slug;
    private String thumbnailUrl;
    private CourseStatus status;
    private String rejectionReason;
    private Integer enrolledCount;
    private Integer completionCount;
    private BigDecimal averageRating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}