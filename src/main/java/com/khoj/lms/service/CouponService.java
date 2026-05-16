package com.khoj.lms.service;

import com.khoj.lms.dto.coupon.*;
import com.khoj.lms.entity.Coupon;
import com.khoj.lms.entity.Course;
import com.khoj.lms.entity.Order;
import com.khoj.lms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CouponService {

    // ───── ADMIN ─────
    CouponResponse create(CouponRequest request, String adminEmail);

    CouponResponse update(UUID id, CouponRequest request, String adminEmail);

    void delete(UUID id, String adminEmail);

    CouponResponse toggleActive(UUID id, String adminEmail);

    Page<CouponResponse> adminList(Boolean active, Pageable pageable);

    CouponResponse adminGetById(UUID id);

    // ───── STUDENT ─────
    /** Returns whether a coupon is valid for a given (student, course), with computed discount. */
    CouponValidationResponse validate(String code, UUID courseId, String studentEmail);

    // ───── INTERNAL — called by OrderService ─────
    /**
     * Resolves a coupon code for a (student, course) and computes the discount.
     * Throws if invalid (so the caller can fail the order).
     * Does NOT increment usage — caller does that on order completion.
     */
    DiscountResult resolveForCheckout(String code, UUID courseId, UUID studentId, BigDecimal originalAmount);

    /** Atomic: increments totalUses + records CouponUsage row. */
    void recordUsage(Coupon coupon, UUID studentId, UUID courseId, UUID orderId, BigDecimal discountApplied);

    Coupon findOrThrow(UUID id);

    /** Tuple-return for resolveForCheckout. */
    record DiscountResult(Coupon coupon, BigDecimal discount, BigDecimal finalAmount) {}


    void recordUsage(Coupon coupon, User student, Course course, Order order, BigDecimal discountApplied);

    // Add these two methods:

    /** Public — list active public coupons (for homepage banners). */
    List<PublicCouponResponse> listPublicActive();

    /** Public/logged-in — list coupons applicable to a specific course. */
    List<PublicCouponResponse> listApplicableForCourse(UUID courseId);

}