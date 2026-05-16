package com.khoj.lms.repository;

import com.khoj.lms.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {

    @Query("""
           SELECT COUNT(cu) FROM CouponUsage cu
           WHERE cu.coupon.id  = :couponId
             AND cu.student.id = :studentId
             AND cu.isDeleted  = false
           """)
    int countByCouponAndStudent(@Param("couponId") UUID couponId,
                                @Param("studentId") UUID studentId);
}