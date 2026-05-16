package com.khoj.lms.dto.coupon;

import com.khoj.lms.enums.CouponType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PublicCouponResponse {

    private UUID   id;
    private String code;
    private String description;

    private CouponType couponType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;

    private LocalDateTime validUntil;       // for "expires in 3 days" badge

    private UUID   scopedCourseId;          // null = applies to any course
    private String scopedCourseTitle;
    private UUID   scopedCategoryId;
    private String scopedCategoryName;
}