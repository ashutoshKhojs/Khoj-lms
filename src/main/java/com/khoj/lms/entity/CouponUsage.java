package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Records every time a coupon is used by a student on a course.
 * Used to enforce per-student usage caps and produce admin reports.
 *
 * Table: coupon_usages
 */
@Entity
@Table(
        name = "coupon_usages",
        indexes = {
                @Index(name = "idx_cu_coupon",  columnList = "coupon_id"),
                @Index(name = "idx_cu_student", columnList = "student_id"),
                @Index(name = "idx_cu_course",  columnList = "course_id"),
                @Index(name = "idx_cu_order",   columnList = "order_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CouponUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "discount_applied", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountApplied;
}