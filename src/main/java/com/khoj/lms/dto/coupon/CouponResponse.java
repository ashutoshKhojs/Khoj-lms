package com.khoj.lms.dto.coupon;

import com.khoj.lms.enums.CouponType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CouponResponse {

    private UUID id;
    private String code;
    private String description;

    private CouponType couponType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    private Integer maxTotalUses;
    private Integer maxUsesPerStudent;
    private Integer totalUses;

    private UUID   scopedCourseId;
    private String scopedCourseTitle;
    private UUID   scopedCategoryId;
    private String scopedCategoryName;

    private Boolean isActive;
    private Boolean isPublic;

    private LocalDateTime createdAt;
}