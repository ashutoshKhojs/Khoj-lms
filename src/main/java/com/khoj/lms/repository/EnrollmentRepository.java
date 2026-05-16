package com.khoj.lms.repository;

import com.khoj.lms.entity.Enrollment;
import com.khoj.lms.enums.EnrollmentStatus;
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
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    // ───── EXISTENCE / LOOKUP ─────
    Optional<Enrollment> findByIdAndIsDeletedFalse(UUID id);

    @Query("""
           SELECT e FROM Enrollment e
           WHERE e.student.id = :studentId
             AND e.course.id  = :courseId
             AND e.isDeleted  = false
           """)
    Optional<Enrollment> findByStudentAndCourse(@Param("studentId") UUID studentId,
                                                @Param("courseId")  UUID courseId);

    @Query("""
           SELECT COUNT(e) > 0 FROM Enrollment e
           WHERE e.student.id = :studentId
             AND e.course.id  = :courseId
             AND e.isDeleted  = false
           """)
    boolean existsByStudentAndCourse(@Param("studentId") UUID studentId,
                                     @Param("courseId")  UUID courseId);

    // ───── STUDENT VIEWS ─────
    @Query("""
           SELECT e FROM Enrollment e
           WHERE e.student.id = :studentId
             AND e.isDeleted = false
             AND (:status IS NULL OR e.status = :status)
           ORDER BY e.lastAccessedAt DESC NULLS LAST, e.createdAt DESC
           """)
    Page<Enrollment> findMyEnrollments(@Param("studentId") UUID studentId,
                                       @Param("status")    EnrollmentStatus status,
                                       Pageable pageable);

    /** "Continue Learning" widget — recent active courses with progress. */
    @Query("""
           SELECT e FROM Enrollment e
           WHERE e.student.id = :studentId
             AND e.status = 'ACTIVE'
             AND e.isDeleted = false
             AND e.lastAccessedAt IS NOT NULL
           ORDER BY e.lastAccessedAt DESC
           """)
    List<Enrollment> findContinueLearning(@Param("studentId") UUID studentId,
                                          Pageable pageable);

    // ───── INSTRUCTOR VIEWS ─────
    @Query("""
           SELECT e FROM Enrollment e
           WHERE e.course.id = :courseId
             AND e.isDeleted = false
           ORDER BY e.createdAt DESC
           """)
    Page<Enrollment> findByCourse(@Param("courseId") UUID courseId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId AND e.isDeleted = false")
    long countByCourse(@Param("courseId") UUID courseId);

    @Query("""
           SELECT COUNT(e) FROM Enrollment e
           WHERE e.course.id = :courseId
             AND e.status = 'COMPLETED'
             AND e.isDeleted = false
           """)
    long countCompletedByCourse(@Param("courseId") UUID courseId);

    @Query("""
           SELECT COALESCE(AVG(e.progressPercentage), 0)
           FROM Enrollment e
           WHERE e.course.id = :courseId
             AND e.isDeleted = false
           """)
    double avgProgressByCourse(@Param("courseId") UUID courseId);

    // ───── ADMIN VIEWS ─────
    @Query("""
           SELECT e FROM Enrollment e
           WHERE e.isDeleted = false
             AND (:status IS NULL OR e.status = :status)
           ORDER BY e.createdAt DESC
           """)
    Page<Enrollment> findAllForAdmin(@Param("status") EnrollmentStatus status, Pageable pageable);

    @Query("""
           SELECT e FROM Enrollment e
           WHERE e.student.id = :studentId
             AND e.isDeleted = false
           ORDER BY e.createdAt DESC
           """)
    Page<Enrollment> findByStudentForAdmin(@Param("studentId") UUID studentId, Pageable pageable);

    // ───── BULK UPDATES (atomic) ─────
    @Modifying
    @Query("""
           UPDATE Enrollment e
           SET e.progressPercentage = :pct,
               e.lessonsCompleted   = :completed,
               e.updatedAt          = CURRENT_TIMESTAMP
           WHERE e.id = :id
           """)
    void updateProgress(@Param("id") UUID id,
                        @Param("pct") double progressPercentage,
                        @Param("completed") int lessonsCompleted);
}