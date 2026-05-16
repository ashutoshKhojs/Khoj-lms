package com.khoj.lms.controller;

import com.khoj.lms.dto.common.ApiResponse;
import com.khoj.lms.dto.enrollment.EnrollRequest;
import com.khoj.lms.dto.order.OrderResponse;
import com.khoj.lms.enums.OrderStatus;
import com.khoj.lms.service.OrderService;
import com.khoj.lms.util.ApiRoutes;
import com.khoj.lms.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Course checkout & payment orders")
public class OrderController {

    private final OrderService orderService;

    // ───── STUDENT ─────

    @PostMapping(ApiRoutes.Order.BASE + ApiRoutes.Order.CREATE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Create an order to enroll in a course (with optional coupon)")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @PathVariable UUID courseId,
            @Valid @RequestBody(required = false) EnrollRequest request,
            @CurrentUser UserDetails user) {

        String coupon = request != null ? request.getCouponCode() : null;

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Order created",
                orderService.createOrder(courseId, coupon, user.getUsername())
        ));
    }

    @PostMapping(ApiRoutes.Order.BASE + ApiRoutes.Order.MARK_PAID)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Mark order as paid (called after payment gateway success)")
    public ResponseEntity<ApiResponse<OrderResponse>> markPaid(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Order marked paid",
                orderService.markPaid(
                        id,
                        body.get("gatewayPaymentId"),
                        body.get("signature"),
                        user.getUsername())
        ));
    }

    @PostMapping(ApiRoutes.Order.BASE + ApiRoutes.Order.CANCEL)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Cancel a PENDING order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Order cancelled",
                orderService.cancel(id, user.getUsername())
        ));
    }

    @GetMapping(ApiRoutes.Order.BASE + ApiRoutes.Order.MY_LIST)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "List my orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> myOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser UserDetails user) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                "Orders fetched",
                orderService.getMyOrders(status, user.getUsername(), pageable)
        ));
    }

    @GetMapping(ApiRoutes.Order.BASE + ApiRoutes.Order.MY_GET)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Get one of my orders")
    public ResponseEntity<ApiResponse<OrderResponse>> myOrder(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Order fetched",
                orderService.getMyOrder(id, user.getUsername())
        ));
    }

    // ───── ADMIN ─────

    @GetMapping(ApiRoutes.Order.ADMIN_BASE + ApiRoutes.Order.ADMIN_LIST)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — list all orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> adminList(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                "Orders fetched (admin)",
                orderService.adminList(status, pageable)
        ));
    }

    @GetMapping(ApiRoutes.Order.ADMIN_BASE + ApiRoutes.Order.ADMIN_GET)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — get any order")
    public ResponseEntity<ApiResponse<OrderResponse>> adminGet(
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.success(
                "Order fetched (admin)",
                orderService.adminGetById(id)
        ));
    }

    @PostMapping(ApiRoutes.Order.ADMIN_BASE + ApiRoutes.Order.ADMIN_REFUND)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — refund a completed order")
    public ResponseEntity<ApiResponse<OrderResponse>> adminRefund(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Order refunded",
                orderService.adminRefund(id, user.getUsername())
        ));
    }
}