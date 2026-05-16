package com.khoj.lms.dto.coupon;

import com.khoj.lms.enums.CouponType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    @Pattern(regexp = "^[A-Z0-9_-]{3,50}$",
            message = "Coupon code must be 3-50 characters, uppercase letters/digits/_/- only")
    private String code;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Coupon type is required")
    private CouponType couponType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be positive")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.00")
    private BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0.00")
    private BigDecimal minOrderAmount;

    @NotNull
    private LocalDateTime validFrom;

    @NotNull
    private LocalDateTime validUntil;

    @Min(1)
    private Integer maxTotalUses;

    @Min(1)
    @Builder.Default
    private Integer maxUsesPerStudent = 1;

    private UUID scopedCourseId;
    private UUID scopedCategoryId;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isPublic = false;
}