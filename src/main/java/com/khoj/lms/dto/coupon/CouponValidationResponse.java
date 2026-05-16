package com.khoj.lms.dto.coupon;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CouponValidationResponse {

    private boolean valid;
    private String  message;

    private String  code;
    private UUID    couponId;

    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
}