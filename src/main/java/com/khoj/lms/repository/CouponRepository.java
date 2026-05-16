package com.khoj.lms.repository;

import com.khoj.lms.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByIdAndIsDeletedFalse(UUID id);

    Optional<Coupon> findByCodeIgnoreCaseAndIsDeletedFalse(String code);

    boolean existsByCodeIgnoreCaseAndIsDeletedFalse(String code);

    @Query("""
           SELECT c FROM Coupon c
           WHERE c.isDeleted = false
             AND (:active IS NULL OR c.isActive = :active)
           ORDER BY c.createdAt DESC
           """)
    Page<Coupon> findAllForAdmin(@Param("active") Boolean active, Pageable pageable);

    @Modifying
    @Query("UPDATE Coupon c SET c.totalUses = c.totalUses + 1 WHERE c.id = :id")
    void incrementTotalUses(@Param("id") UUID id);



    @Query("""
       SELECT c FROM Coupon c
       WHERE c.isDeleted   = false
         AND c.isActive    = true
         AND c.isPublic    = true
         AND c.validFrom   <= CURRENT_TIMESTAMP
         AND c.validUntil  >= CURRENT_TIMESTAMP
         AND (c.maxTotalUses IS NULL OR c.totalUses < c.maxTotalUses)
       ORDER BY c.validUntil ASC
       """)
    List<Coupon> findAllPublicActive();

    @Query("""
       SELECT c FROM Coupon c
       WHERE c.isDeleted   = false
         AND c.isActive    = true
         AND c.isPublic    = true
         AND c.validFrom   <= CURRENT_TIMESTAMP
         AND c.validUntil  >= CURRENT_TIMESTAMP
         AND (c.maxTotalUses IS NULL OR c.totalUses < c.maxTotalUses)
         AND (c.scopedCourse    IS NULL OR c.scopedCourse.id    = :courseId)
         AND (c.scopedCategory  IS NULL OR c.scopedCategory.id  = :categoryId)
       ORDER BY c.validUntil ASC
       """)
    List<Coupon> findApplicableForCourse(@Param("courseId") UUID courseId,
                                         @Param("categoryId") UUID categoryId);
}