package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import com.khoj.lms.enums.CouponType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A discount coupon. Admin creates; students apply at checkout.
 *
 * Examples:
 *   KHOJ100  → 100% off, valid 30 days, max 500 uses, 1 per student
 *   STUDENT50 → 50% off any course, no limit
 *   AIBASICS  → ₹999 off, scoped to one specific course
 *
 * Table: coupons
 */
@Entity
@Table(
        name = "coupons",
        indexes = {
                @Index(name = "idx_coupon_code",   columnList = "code", unique = true),
                @Index(name = "idx_coupon_active", columnList = "is_active"),
                @Index(name = "idx_coupon_validity", columnList = "valid_from, valid_until")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coupon extends BaseEntity {

    /** Stored UPPERCASE — e.g. "KHOJ100" */
    @NotBlank
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_type", nullable = false, length = 20)
    private CouponType couponType;

    /** PERCENTAGE: 0–100 ; FIXED: rupees off (e.g. 200.00) */
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    /** Max ₹ amount that can be discounted (cap for PERCENTAGE coupons). null = no cap. */
    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    /** Minimum order value required to apply. null = no minimum. */
    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    // ─────────────────────────────────────────
    // Validity Window
    // ─────────────────────────────────────────

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    // ─────────────────────────────────────────
    // Usage Limits
    // ─────────────────────────────────────────

    /** Total platform-wide cap. null = unlimited. */
    @Column(name = "max_total_uses")
    private Integer maxTotalUses;

    /** Per-student cap. null = unlimited. */
    @Column(name = "max_uses_per_student")
    @Builder.Default
    private Integer maxUsesPerStudent = 1;

    @Column(name = "total_uses", nullable = false)
    @Builder.Default
    private Integer totalUses = 0;

    // ─────────────────────────────────────────
    // Scope (null fields = applies to all)
    // ─────────────────────────────────────────

    /** If set, coupon only applies to this specific course. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scoped_course_id")
    private Course scopedCourse;

    /** If set, coupon only applies to courses in this category. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scoped_category_id")
    private Category scopedCategory;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────

    public boolean isWithinValidityWindow() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(validFrom) && !now.isAfter(validUntil);
    }

    public boolean hasReachedTotalLimit() {
        return maxTotalUses != null && totalUses >= maxTotalUses;
    }
}