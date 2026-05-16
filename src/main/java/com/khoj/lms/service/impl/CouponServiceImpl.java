package com.khoj.lms.service.impl;

import com.khoj.lms.audit.AuditLogger;
import com.khoj.lms.dto.coupon.*;
import com.khoj.lms.entity.*;
import com.khoj.lms.enums.CouponType;
import com.khoj.lms.exception.*;
import com.khoj.lms.repository.*;
import com.khoj.lms.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponServiceImpl implements CouponService {

    private final CouponRepository      couponRepository;
    private final CouponUsageRepository usageRepository;
    private final CourseRepository      courseRepository;
    private final CategoryRepository    categoryRepository;
    private final UserRepository        userRepository;
    private final AuditLogger           auditLogger;

    // ═══════════════════════════════════════════════════════════
    // ADMIN
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public CouponResponse create(CouponRequest request, String adminEmail) {
        log.info("Creating coupon code='{}' admin={}", request.getCode(), adminEmail);

        validateRequest(request);

        String normalized = request.getCode().toUpperCase().trim();
        if (couponRepository.existsByCodeIgnoreCaseAndIsDeletedFalse(normalized)) {
            throw new DuplicateResourceException("Coupon code already exists: " + normalized);
        }

        Coupon coupon = Coupon.builder()
                .code(normalized)
                .description(request.getDescription())
                .couponType(request.getCouponType())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderAmount(request.getMinOrderAmount())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .maxTotalUses(request.getMaxTotalUses())
                .maxUsesPerStudent(request.getMaxUsesPerStudent() != null
                        ? request.getMaxUsesPerStudent() : 1)
                .scopedCourse(resolveCourse(request.getScopedCourseId()))
                .scopedCategory(resolveCategory(request.getScopedCategoryId()))
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .totalUses(0)
                .build();

        coupon = couponRepository.save(coupon);

        log.info("Coupon created: id={} code='{}' type={} value={}",
                coupon.getId(), coupon.getCode(), coupon.getCouponType(), coupon.getDiscountValue());

        auditLogger.adminAction(adminEmail, "COUPON_CREATED: " + coupon.getCode(), null);
        return toResponse(coupon);
    }

    @Override
    @Transactional
    public CouponResponse update(UUID id, CouponRequest request, String adminEmail) {
        log.info("Updating coupon id={} admin={}", id, adminEmail);

        Coupon coupon = findOrThrow(id);
        validateRequest(request);

        // Code can change but must remain unique
        String newCode = request.getCode().toUpperCase().trim();
        if (!coupon.getCode().equalsIgnoreCase(newCode)
                && couponRepository.existsByCodeIgnoreCaseAndIsDeletedFalse(newCode)) {
            throw new DuplicateResourceException("Coupon code already exists: " + newCode);
        }

        coupon.setCode(newCode);
        coupon.setDescription(request.getDescription());
        coupon.setCouponType(request.getCouponType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());
        coupon.setMaxTotalUses(request.getMaxTotalUses());
        if (request.getIsPublic() != null) coupon.setIsPublic(request.getIsPublic());
        if (request.getMaxUsesPerStudent() != null)
            coupon.setMaxUsesPerStudent(request.getMaxUsesPerStudent());
        coupon.setScopedCourse(resolveCourse(request.getScopedCourseId()));
        coupon.setScopedCategory(resolveCategory(request.getScopedCategoryId()));
        if (request.getIsActive() != null)
            coupon.setIsActive(request.getIsActive());

        couponRepository.save(coupon);
        log.info("Coupon updated: id={} code='{}'", coupon.getId(), coupon.getCode());
        auditLogger.adminAction(adminEmail, "COUPON_UPDATED: " + coupon.getCode(), null);
        return toResponse(coupon);
    }

    @Override
    @Transactional
    public void delete(UUID id, String adminEmail) {
        Coupon coupon = findOrThrow(id);
        coupon.softDelete();
        couponRepository.save(coupon);

        log.info("Coupon soft-deleted: id={} code='{}'", id, coupon.getCode());
        auditLogger.adminAction(adminEmail, "COUPON_DELETED: " + coupon.getCode(), null);
    }

    @Override
    @Transactional
    public CouponResponse toggleActive(UUID id, String adminEmail) {
        Coupon coupon = findOrThrow(id);
        boolean was = Boolean.TRUE.equals(coupon.getIsActive());
        coupon.setIsActive(!was);
        couponRepository.save(coupon);

        log.info("Coupon active toggled: id={} code='{}' {} → {}",
                id, coupon.getCode(), was, !was);
        auditLogger.adminAction(adminEmail, "COUPON_TOGGLE_ACTIVE: " + coupon.getCode(), null);
        return toResponse(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CouponResponse> adminList(Boolean active, Pageable pageable) {
        return couponRepository.findAllForAdmin(active, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse adminGetById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    // ═══════════════════════════════════════════════════════════
    // STUDENT — validation (no side effects)
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validate(String code, UUID courseId, String studentEmail) {
        log.debug("Validating coupon code='{}' courseId={} student={}",
                code, courseId, studentEmail);

        User student = findUserByEmail(studentEmail);
        Course course = findCourseOrThrowPublished(courseId);

        try {
            DiscountResult result = resolveForCheckout(code, courseId, student.getId(),
                    course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO);

            return CouponValidationResponse.builder()
                    .valid(true)
                    .message("Coupon applied")
                    .code(result.coupon().getCode())
                    .couponId(result.coupon().getId())
                    .originalAmount(course.getPrice())
                    .discountAmount(result.discount())
                    .finalAmount(result.finalAmount())
                    .build();

        } catch (BadRequestException | ResourceNotFoundException ex) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .message(ex.getMessage())
                    .code(code)
                    .originalAmount(course.getPrice())
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(course.getPrice())
                    .build();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // INTERNAL — checkout
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public DiscountResult resolveForCheckout(String code, UUID courseId, UUID studentId,
                                             BigDecimal originalAmount) {

        if (code == null || code.trim().isEmpty()) {
            throw new BadRequestException("Coupon code is required");
        }

        Coupon coupon = couponRepository
                .findByCodeIgnoreCaseAndIsDeletedFalse(code.trim())
                .orElseThrow(() -> new BadRequestException("Invalid coupon code"));

        // 1. Active
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new BadRequestException("This coupon is not active");
        }
        // 2. Validity window
        if (!coupon.isWithinValidityWindow()) {
            throw new BadRequestException("This coupon has expired or is not yet valid");
        }
        // 3. Total usage cap
        if (coupon.hasReachedTotalLimit()) {
            throw new BadRequestException("This coupon has reached its usage limit");
        }
        // 4. Per-student cap
        int used = usageRepository.countByCouponAndStudent(coupon.getId(), studentId);
        if (coupon.getMaxUsesPerStudent() != null && used >= coupon.getMaxUsesPerStudent()) {
            throw new BadRequestException("You have already used this coupon");
        }
        // 5. Scope: course
        if (coupon.getScopedCourse() != null
                && !coupon.getScopedCourse().getId().equals(courseId)) {
            throw new BadRequestException("This coupon is not applicable to this course");
        }
        // 6. Scope: category
        if (coupon.getScopedCategory() != null) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
            if (course.getCategory() == null
                    || !course.getCategory().getId().equals(coupon.getScopedCategory().getId())) {
                throw new BadRequestException("This coupon is not applicable to this course's category");
            }
        }
        // 7. Minimum order
        if (coupon.getMinOrderAmount() != null
                && originalAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BadRequestException(
                    "Minimum order amount of ₹" + coupon.getMinOrderAmount() + " required");
        }

        // ───── Compute discount ─────
        BigDecimal discount = computeDiscount(coupon, originalAmount);
        BigDecimal finalAmount = originalAmount.subtract(discount);

        // Never go below zero
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            discount = originalAmount;
            finalAmount = BigDecimal.ZERO;
        }

        return new DiscountResult(coupon, discount, finalAmount);
    }

    @Override
    @Transactional
    public void recordUsage(Coupon coupon, UUID studentId, UUID courseId,
                            UUID orderId, BigDecimal discountApplied) {

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        // Order is loaded by OrderService — here we trust the reference id.
        // Re-fetch to attach managed entity:
        // (We accept that this couples Coupon→Order; both live in same module.)

        CouponUsage usage = CouponUsage.builder()
                .coupon(coupon)
                .student(student)
                .course(course)
                .order(em(orderId))
                .discountApplied(discountApplied)
                .build();
        usageRepository.save(usage);

        couponRepository.incrementTotalUses(coupon.getId());

        log.info("Coupon usage recorded: couponId={} studentId={} orderId={} discount={}",
                coupon.getId(), studentId, orderId, discountApplied);
    }

    @Override
    public Coupon findOrThrow(UUID id) {
        return couponRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
    }


    // In CouponServiceImpl:
    @Override
    @Transactional
    public void recordUsage(Coupon coupon, User student, Course course, Order order, BigDecimal discountApplied) {
        CouponUsage usage = CouponUsage.builder()
                .coupon(coupon).student(student).course(course)
                .order(order).discountApplied(discountApplied)
                .build();
        usageRepository.save(usage);
        couponRepository.incrementTotalUses(coupon.getId());

        log.info("Coupon usage recorded: couponId={} studentId={} orderId={} discount={}",
                coupon.getId(), student.getId(), order.getId(), discountApplied);
    }


    @Override
    @Transactional(readOnly = true)
    public List<PublicCouponResponse> listPublicActive() {
        log.debug("[PUBLIC] Listing active public coupons");
        return couponRepository.findAllPublicActive()
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicCouponResponse> listApplicableForCourse(UUID courseId) {
        log.debug("[PUBLIC] Listing coupons applicable to courseId={}", courseId);

        Course course = courseRepository.findById(courseId)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (!course.isPublished()) {
            throw new BadRequestException("Course is not published");
        }

        UUID categoryId = course.getCategory() != null ? course.getCategory().getId() : null;

        return couponRepository.findApplicableForCourse(courseId, categoryId)
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════




    private BigDecimal computeDiscount(Coupon coupon, BigDecimal originalAmount) {
        BigDecimal discount;
        if (coupon.getCouponType() == CouponType.PERCENTAGE) {
            discount = originalAmount
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else { // FIXED
            discount = coupon.getDiscountValue();
        }
        // Cap (percentage coupons with maxDiscountAmount)
        if (coupon.getMaxDiscountAmount() != null
                && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discount = coupon.getMaxDiscountAmount();
        }
        // Cannot exceed original
        if (discount.compareTo(originalAmount) > 0) {
            discount = originalAmount;
        }
        return discount;
    }

    private void validateRequest(CouponRequest r) {
        if (r.getValidFrom().isAfter(r.getValidUntil())) {
            throw new BadRequestException("validFrom must be before validUntil");
        }
        if (r.getCouponType() == CouponType.PERCENTAGE
                && r.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("PERCENTAGE discount cannot exceed 100");
        }
        if (r.getScopedCourseId() != null && r.getScopedCategoryId() != null) {
            throw new BadRequestException(
                    "Coupon can be scoped to either a course OR a category, not both");
        }
    }

    private Course resolveCourse(UUID id) {
        if (id == null) return null;
        return courseRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));
    }

    private Category resolveCategory(UUID id) {
        if (id == null) return null;
        return categoryRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    private Course findCourseOrThrowPublished(UUID id) {
        Course course = courseRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));
        if (!course.isPublished()) {
            throw new BadRequestException("Course is not published");
        }
        return course;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /** Re-fetch a managed Order reference by id. */
    private Order em(UUID orderId) {
        // Inject OrderRepository here when wiring — keeping cycle clean we use a fresh lookup.
        // (Simpler: see note below — call recordUsage AFTER order is saved.)
        throw new IllegalStateException(
                "recordUsage must be called via OrderService — never standalone.");
    }

    private CouponResponse toResponse(Coupon c) {
        return CouponResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .description(c.getDescription())
                .couponType(c.getCouponType())
                .discountValue(c.getDiscountValue())
                .maxDiscountAmount(c.getMaxDiscountAmount())
                .minOrderAmount(c.getMinOrderAmount())
                .validFrom(c.getValidFrom())
                .validUntil(c.getValidUntil())
                .maxTotalUses(c.getMaxTotalUses())
                .maxUsesPerStudent(c.getMaxUsesPerStudent())
                .totalUses(c.getTotalUses())
                .scopedCourseId(c.getScopedCourse() != null ? c.getScopedCourse().getId() : null)
                .scopedCourseTitle(c.getScopedCourse() != null ? c.getScopedCourse().getTitle() : null)
                .scopedCategoryId(c.getScopedCategory() != null ? c.getScopedCategory().getId() : null)
                .scopedCategoryName(c.getScopedCategory() != null ? c.getScopedCategory().getName() : null)
                .isActive(c.getIsActive())
                .isPublic(c.getIsPublic())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private PublicCouponResponse toPublicResponse(Coupon c) {
        return PublicCouponResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .description(c.getDescription())
                .couponType(c.getCouponType())
                .discountValue(c.getDiscountValue())
                .maxDiscountAmount(c.getMaxDiscountAmount())
                .minOrderAmount(c.getMinOrderAmount())
                .validUntil(c.getValidUntil())
                .scopedCourseId(c.getScopedCourse() != null ? c.getScopedCourse().getId() : null)
                .scopedCourseTitle(c.getScopedCourse() != null ? c.getScopedCourse().getTitle() : null)
                .scopedCategoryId(c.getScopedCategory() != null ? c.getScopedCategory().getId() : null)
                .scopedCategoryName(c.getScopedCategory() != null ? c.getScopedCategory().getName() : null)
                .build();
    }
}