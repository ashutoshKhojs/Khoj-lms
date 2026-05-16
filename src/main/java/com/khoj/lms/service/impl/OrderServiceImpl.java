package com.khoj.lms.service.impl;

import com.khoj.lms.audit.AuditLogger;
import com.khoj.lms.dto.order.OrderResponse;
import com.khoj.lms.entity.*;
import com.khoj.lms.enums.CourseStatus;
import com.khoj.lms.enums.OrderStatus;
import com.khoj.lms.exception.*;
import com.khoj.lms.repository.*;
import com.khoj.lms.service.CouponService;
import com.khoj.lms.service.CouponService.DiscountResult;
import com.khoj.lms.service.EmailService;
import com.khoj.lms.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository       orderRepository;
    private final CourseRepository      courseRepository;
    private final UserRepository        userRepository;
    private final EnrollmentRepository  enrollmentRepository;
    private final CouponService         couponService;
    private final EmailService          emailService;
    private final AuditLogger           auditLogger;

    // ═══════════════════════════════════════════════════════════
    // STUDENT
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public OrderResponse createOrder(UUID courseId, String couponCode, String studentEmail) {
        log.info("Create order: student={} courseId={} couponCode={}",
                studentEmail, courseId, couponCode);

        User student = findUserByEmail(studentEmail);
        Course course = findCourseOrThrow(courseId);

        // Gate 1 — only published courses
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new BadRequestException("This course is not available for purchase.");
        }
        // Gate 2 — can't buy own course
        if (course.getInstructor().getId().equals(student.getId())) {
            throw new BadRequestException("You cannot enroll in your own course.");
        }
        // Gate 3 — already enrolled?
        if (enrollmentRepository.existsByStudentAndCourse(student.getId(), course.getId())) {
            throw new DuplicateResourceException("You are already enrolled in this course.");
        }
        // Gate 4 — has any completed order for this course already?
        if (orderRepository.hasCompletedOrderForCourse(student.getId(), course.getId())) {
            throw new DuplicateResourceException(
                    "You already have a completed order for this course.");
        }

        BigDecimal original = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal finalAmt = original;
        Coupon appliedCoupon = null;

        // ───── Apply coupon if provided ─────
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            DiscountResult res = couponService.resolveForCheckout(
                    couponCode, courseId, student.getId(), original);
            appliedCoupon = res.coupon();
            discount = res.discount();
            finalAmt = res.finalAmount();

            log.info("Coupon applied: code='{}' discount={} finalAmount={}",
                    appliedCoupon.getCode(), discount, finalAmt);
        }

        // ───── Build order ─────
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .student(student)
                .course(course)
                .originalAmount(original)
                .discountAmount(discount)
                .finalAmount(finalAmt)
                .coupon(appliedCoupon)
                .couponCode(appliedCoupon != null ? appliedCoupon.getCode() : null)
                .status(OrderStatus.PENDING)
                .paymentGateway(finalAmt.compareTo(BigDecimal.ZERO) == 0 ? "FREE" : null)
                .build();

        order = orderRepository.save(order);

        log.info("Order created: id={} orderNumber={} student={} courseId={} original={} discount={} final={}",
                order.getId(), order.getOrderNumber(), studentEmail, courseId,
                original, discount, finalAmt);

        // ───── Auto-complete if free (₹0) ─────
        if (order.isFree()) {
            order.markCompleted();
            order = orderRepository.save(order);

            if (appliedCoupon != null) {
                couponService.recordUsage(appliedCoupon, student, course, order, discount);
            }

            createEnrollmentFromOrder(order);

            log.info("Order auto-completed (free): id={} orderNumber={}",
                    order.getId(), order.getOrderNumber());

            auditLogger.adminAction(studentEmail,
                    "ORDER_COMPLETED (free): " + order.getOrderNumber()
                            + " course=" + course.getTitle(), null);
        } else {
            // Paid path — Phase 2 will create a Razorpay/Stripe order here
            // and return its id. For now, order stays PENDING for /mark-paid.
            log.info("Order awaiting payment: id={} amount={}",
                    order.getId(), finalAmt);
        }

        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse markPaid(UUID orderId, String gatewayPaymentId,
                                  String signature, String studentEmail) {
        log.info("Mark paid: orderId={} student={} paymentId={}",
                orderId, studentEmail, gatewayPaymentId);

        User student = findUserByEmail(studentEmail);
        Order order = findOrThrow(orderId);

        assertOwns(order, student);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException(
                    "Order is in " + order.getStatus() + " status — cannot mark paid.");
        }

        // TODO Phase 2: verify signature with Razorpay/Stripe SDK here
        order.setGatewayPaymentId(gatewayPaymentId);
        order.setGatewaySignature(signature);
        order.markCompleted();
        orderRepository.save(order);

        if (order.getCoupon() != null) {
            couponService.recordUsage(order.getCoupon(), student, order.getCourse(),
                    order, order.getDiscountAmount());
        }

        createEnrollmentFromOrder(order);

        log.info("Order completed (paid): id={} orderNumber={}",
                orderId, order.getOrderNumber());
        auditLogger.adminAction(studentEmail,
                "ORDER_COMPLETED (paid): " + order.getOrderNumber()
                        + " amount=" + order.getFinalAmount(), null);

        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancel(UUID orderId, String studentEmail) {
        User student = findUserByEmail(studentEmail);
        Order order = findOrThrow(orderId);
        assertOwns(order, student);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException(
                    "Only PENDING orders can be cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("Order cancelled: id={} student={}", orderId, studentEmail);
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(UUID orderId, String studentEmail) {
        User student = findUserByEmail(studentEmail);
        Order order = findOrThrow(orderId);
        assertOwns(order, student);
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(OrderStatus status, String studentEmail,
                                           Pageable pageable) {
        User student = findUserByEmail(studentEmail);
        return orderRepository.findMyOrders(student.getId(), status, pageable)
                .map(this::toResponse);
    }

    // ═══════════════════════════════════════════════════════════
    // ADMIN
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> adminList(OrderStatus status, Pageable pageable) {
        return orderRepository.findAllForAdmin(status, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse adminGetById(UUID orderId) {
        return toResponse(findOrThrow(orderId));
    }

    @Override
    @Transactional
    public OrderResponse adminRefund(UUID orderId, String adminEmail) {
        Order order = findOrThrow(orderId);
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BadRequestException("Only COMPLETED orders can be refunded.");
        }
        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);

        log.info("[ADMIN] Order refunded: id={} by={}", orderId, adminEmail);
        auditLogger.adminAction(adminEmail,
                "ORDER_REFUNDED: " + order.getOrderNumber(), null);
        return toResponse(order);
    }

    @Override
    public Order findOrThrow(UUID id) {
        return orderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Creates the actual Enrollment row once an order is COMPLETED.
     * This is the ONLY path that creates enrollments.
     */
    private void createEnrollmentFromOrder(Order order) {
        // Defensive — double-create guard
        if (enrollmentRepository.existsByStudentAndCourse(
                order.getStudent().getId(), order.getCourse().getId())) {
            log.warn("Enrollment already exists — skipping: orderId={}", order.getId());
            return;
        }

        Enrollment enrollment = Enrollment.builder()
                .student(order.getStudent())
                .course(order.getCourse())
                .status(com.khoj.lms.enums.EnrollmentStatus.ACTIVE)
                .progressPercentage(0.0)
                .lessonsCompleted(0)
                .totalTimeSpentSeconds(0L)
                .isCertificateIssued(false)
                .build();

        enrollmentRepository.save(enrollment);

        // Atomic counter
        courseRepository.incrementEnrolledCount(order.getCourse().getId());

        emailService.sendCourseEnrollmentEmail(
                order.getStudent().getEmail(),
                order.getStudent().getFullName(),
                order.getCourse().getTitle(),
                order.getCourse().getInstructor().getFullName()
        );


        log.info("Enrollment created from order: orderId={} student={} courseId={}",
                order.getId(), order.getStudent().getEmail(), order.getCourse().getId());



    }

    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "KHOJ-" + date + "-" + suffix;
    }

    private void assertOwns(Order order, User student) {
        if (!order.getStudent().getId().equals(student.getId())) {
            log.warn("Order ownership check failed: orderId={} expected={} actual={}",
                    order.getId(), order.getStudent().getId(), student.getId());
            throw new AccessDeniedException("You do not own this order.");
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Course findCourseOrThrow(UUID id) {
        return courseRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));
    }

    private OrderResponse toResponse(Order o) {
        UUID enrollmentId = null;
        if (o.getStatus() == OrderStatus.COMPLETED) {
            enrollmentId = enrollmentRepository
                    .findByStudentAndCourse(o.getStudent().getId(), o.getCourse().getId())
                    .map(Enrollment::getId)
                    .orElse(null);
        }

        return OrderResponse.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .courseId(o.getCourse().getId())
                .courseTitle(o.getCourse().getTitle())
                .courseThumbnailUrl(o.getCourse().getThumbnailUrl())
                .studentId(o.getStudent().getId())
                .studentName(o.getStudent().getFullName())
                .studentEmail(o.getStudent().getEmail())
                .originalAmount(o.getOriginalAmount())
                .discountAmount(o.getDiscountAmount())
                .finalAmount(o.getFinalAmount())
                .couponCode(o.getCouponCode())
                .couponId(o.getCoupon() != null ? o.getCoupon().getId() : null)
                .status(o.getStatus())
                .completedAt(o.getCompletedAt())
                .failureReason(o.getFailureReason())
                .paymentGateway(o.getPaymentGateway())
                .gatewayOrderId(o.getGatewayOrderId())
                .gatewayPaymentId(o.getGatewayPaymentId())
                .createdAt(o.getCreatedAt())
                .enrollmentId(enrollmentId)
                .build();
    }
}