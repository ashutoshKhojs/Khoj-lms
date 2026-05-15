package com.khoj.lms.repository;

import com.khoj.lms.entity.Course;
import com.khoj.lms.enums.CourseStatus;
import com.khoj.lms.enums.DifficultyLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID>,
        JpaSpecificationExecutor<Course> {

    Optional<Course> findBySlugAndIsDeletedFalse(String slug);

    boolean existsBySlugAndIsDeletedFalse(String slug);

    // ─── Public listing ──────────────────────────────────────────────────
    @Query("""
        SELECT c FROM Course c
        WHERE c.isDeleted = false
          AND c.status = 'PUBLISHED'
          AND (:search   IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:catId    IS NULL OR c.category.id  = :catId)
          AND (:diff     IS NULL OR c.difficultyLevel = :diff)
          AND (:language IS NULL OR LOWER(c.language) = LOWER(:language))
          AND (:isFree   IS NULL OR c.isFree = :isFree)
        """)
    Page<Course> findPublished(
            @Param("search")   String search,
            @Param("catId")    UUID categoryId,
            @Param("diff")     DifficultyLevel difficulty,
            @Param("language") String language,
            @Param("isFree")   Boolean isFree,
            Pageable pageable
    );

    // ─── Instructor's own courses ─────────────────────────────────────────
    @Query("""
        SELECT c FROM Course c
        WHERE c.instructor.id = :instructorId
          AND c.isDeleted = false
        """)
    Page<Course> findByInstructor(
            @Param("instructorId") UUID instructorId,
            Pageable pageable);

    // ─── Admin — all courses with optional status filter ──────────────────
    @Query("""
        SELECT c FROM Course c
        WHERE c.isDeleted = false
          AND (:status IS NULL OR c.status = :status)
        """)
    Page<Course> findAllForAdmin(
            @Param("status") CourseStatus status,
            Pageable pageable);

    // ─── Stats update ─────────────────────────────────────────────────────
    @Modifying
    @Query("""
        UPDATE Course c
        SET c.totalModules        = :modules,
            c.totalLessons        = :lessons,
            c.totalDurationSeconds = :duration,
            c.updatedAt           = CURRENT_TIMESTAMP
        WHERE c.id = :id
        """)
    void updateStats(
            @Param("id")       UUID id,
            @Param("modules")  int modules,
            @Param("lessons")  int lessons,
            @Param("duration") long duration
    );

    @Modifying
    @Query("UPDATE Course c SET c.enrolledCount = c.enrolledCount + 1 WHERE c.id = :id")
    void incrementEnrolledCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Course c SET c.completionCount = c.completionCount + 1 WHERE c.id = :id")
    void incrementCompletionCount(@Param("id") UUID id);

    // ─────────────────────────────────────────────────────────────────────
    // FIXED — fetch modules only (NOT lessons simultaneously)
    // Lessons load lazily inside the @Transactional service method.
    // This avoids MultipleBagFetchException / Cartesian product issues.
    // ─────────────────────────────────────────────────────────────────────

    @Query("""
        SELECT DISTINCT c FROM Course c
        LEFT JOIN FETCH c.modules m
        LEFT JOIN FETCH c.category
        LEFT JOIN FETCH c.instructor
        WHERE c.slug = :slug
          AND c.isDeleted = false
        """)
    Optional<Course> findBySlugWithModules(@Param("slug") String slug);

    @Query("""
        SELECT DISTINCT c FROM Course c
        LEFT JOIN FETCH c.modules m
        LEFT JOIN FETCH c.category
        LEFT JOIN FETCH c.instructor
        WHERE c.id = :id
          AND c.isDeleted = false
        """)
    Optional<Course> findByIdWithModules(@Param("id") UUID id);
}