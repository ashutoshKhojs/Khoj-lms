package com.khoj.lms.dto.course;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CourseRejectionRequest {

    @NotBlank
    @Size(max = 500)
    private String reason;
}