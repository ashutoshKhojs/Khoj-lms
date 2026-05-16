package com.khoj.lms.service;

import com.khoj.lms.dto.order.OrderResponse;
import com.khoj.lms.entity.Order;
import com.khoj.lms.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    /** Creates an order for (student, course) with optional coupon, auto-completing free orders. */
    OrderResponse createOrder(UUID courseId, String couponCode, String studentEmail);

    /** Phase 2: payment gateway callback to mark as COMPLETED. Free orders skip this. */
    OrderResponse markPaid(UUID orderId, String gatewayPaymentId, String signature, String studentEmail);

    OrderResponse cancel(UUID orderId, String studentEmail);

    OrderResponse getMyOrder(UUID orderId, String studentEmail);

    Page<OrderResponse> getMyOrders(OrderStatus status, String studentEmail, Pageable pageable);

    // Admin
    Page<OrderResponse> adminList(OrderStatus status, Pageable pageable);

    OrderResponse adminGetById(UUID orderId);

    OrderResponse adminRefund(UUID orderId, String adminEmail);

    Order findOrThrow(UUID id);
}