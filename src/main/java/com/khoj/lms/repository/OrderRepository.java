package com.khoj.lms.repository;

import com.khoj.lms.entity.Order;
import com.khoj.lms.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndIsDeletedFalse(UUID id);

    Optional<Order> findByOrderNumberAndIsDeletedFalse(String orderNumber);

    @Query("""
           SELECT COUNT(o) > 0 FROM Order o
           WHERE o.student.id = :studentId
             AND o.course.id  = :courseId
             AND o.status     = 'COMPLETED'
             AND o.isDeleted  = false
           """)
    boolean hasCompletedOrderForCourse(@Param("studentId") UUID studentId,
                                       @Param("courseId")  UUID courseId);

    @Query("""
           SELECT o FROM Order o
           WHERE o.student.id = :studentId
             AND o.isDeleted = false
             AND (:status IS NULL OR o.status = :status)
           ORDER BY o.createdAt DESC
           """)
    Page<Order> findMyOrders(@Param("studentId") UUID studentId,
                             @Param("status") OrderStatus status,
                             Pageable pageable);

    @Query("""
           SELECT o FROM Order o
           WHERE o.isDeleted = false
             AND (:status IS NULL OR o.status = :status)
           ORDER BY o.createdAt DESC
           """)
    Page<Order> findAllForAdmin(@Param("status") OrderStatus status, Pageable pageable);
}