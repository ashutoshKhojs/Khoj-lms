package com.khoj.lms.dto.course;

import com.khoj.lms.enums.CourseStatus;
import com.khoj.lms.enums.DifficultyLevel;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseFilter {

    private String search;
    private UUID categoryId;
    private DifficultyLevel difficulty;
    private String language;
    private Boolean isFree;
    private String instructorId;
    private CourseStatus status;
}