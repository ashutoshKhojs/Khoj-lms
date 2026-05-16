package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import com.khoj.lms.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A purchase (or free claim) of a course by a student.
 *
 * Lifecycle:  PENDING → COMPLETED (success)
 *                    → FAILED / CANCELLED
 *                    → REFUNDED (after COMPLETED)
 *
 * For free courses (price=0) and 100%-off coupons → auto-completed on creation.
 *
 * Table: orders
 */
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_order_student", columnList = "student_id"),
                @Index(name = "idx_order_course",  columnList = "course_id"),
                @Index(name = "idx_order_status",  columnList = "status"),
                @Index(name = "idx_order_number",  columnList = "order_number", unique = true)
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order extends BaseEntity {

    /** Human-readable unique order number — e.g. "KHOJ-20260516-A1B2C3" */
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // ─────────────────────────────────────────
    // Amounts (snapshot at checkout, never recalculated)
    // ─────────────────────────────────────────

    @Column(name = "original_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount;

    // ─────────────────────────────────────────
    // Coupon (snapshot)
    // ─────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    // ─────────────────────────────────────────
    // Status
    // ─────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    // ─────────────────────────────────────────
    // Payment Gateway (Phase 2 — Razorpay/Stripe)
    // ─────────────────────────────────────────

    @Column(name = "payment_gateway", length = 50)
    private String paymentGateway;       // "RAZORPAY" | "STRIPE" | "FREE"

    @Column(name = "gateway_order_id", length = 100)
    private String gatewayOrderId;

    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    @Column(name = "gateway_signature", length = 200)
    private String gatewaySignature;

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────

    public boolean isFree() {
        return finalAmount == null || finalAmount.compareTo(BigDecimal.ZERO) == 0;
    }

    public void markCompleted() {
        this.status = OrderStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}