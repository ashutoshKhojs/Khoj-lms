package com.khoj.lms.dto.enrollment;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnrollRequest {

    /** Optional — applied at checkout. Empty/null = no coupon. */
    @Size(max = 50)
    private String couponCode;
}