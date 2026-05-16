package com.khoj.lms.dto.order;

import com.khoj.lms.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderResponse {

    private UUID id;
    private String orderNumber;

    private UUID   courseId;
    private String courseTitle;
    private String courseThumbnailUrl;

    private UUID   studentId;
    private String studentName;
    private String studentEmail;

    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    private String couponCode;
    private UUID   couponId;

    private OrderStatus status;
    private LocalDateTime completedAt;
    private String failureReason;

    private String paymentGateway;
    private String gatewayOrderId;
    private String gatewayPaymentId;

    private LocalDateTime createdAt;

    // Helpful flag for frontend
    private UUID enrollmentId;  // populated once enrollment is created
}