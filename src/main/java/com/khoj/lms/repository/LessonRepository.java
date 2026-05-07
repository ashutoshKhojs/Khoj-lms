package com.khoj.lms.repository;

import com.khoj.lms.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findByModuleIdAndIsDeletedFalseOrderByDisplayOrderAsc(UUID moduleId);

    List<Lesson> findByCourseIdAndIsDeletedFalseOrderByDisplayOrderAsc(UUID courseId);

    Optional<Lesson> findByIdAndIsDeletedFalse(UUID id);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.course.id = :courseId AND l.isDeleted = false")
    int countByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.module.id = :moduleId AND l.isDeleted = false")
    int countByModuleId(@Param("moduleId") UUID moduleId);

    @Query("""
        SELECT COALESCE(SUM(l.videoDurationSeconds), 0)
        FROM Lesson l
        WHERE l.course.id = :courseId AND l.isDeleted = false
        """)
    long sumDurationByCourseId(@Param("courseId") UUID courseId);

    @Query("""
        SELECT COALESCE(SUM(l.videoDurationSeconds), 0)
        FROM Lesson l
        WHERE l.module.id = :moduleId AND l.isDeleted = false
        """)
    long sumDurationByModuleId(@Param("moduleId") UUID moduleId);

    @Query("SELECT MAX(l.displayOrder) FROM Lesson l WHERE l.module.id = :moduleId AND l.isDeleted = false")
    Integer findMaxDisplayOrder(@Param("moduleId") UUID moduleId);

    /**
     * Returns the next lesson in the same module (for "Next" button in player).
     */
    @Query("""
        SELECT l FROM Lesson l
        WHERE l.module.id = :moduleId
          AND l.displayOrder > :currentOrder
          AND l.isDeleted = false
          AND l.isPublished = true
        ORDER BY l.displayOrder ASC
        LIMIT 1
        """)
    Optional<Lesson> findNextLesson(@Param("moduleId") UUID moduleId,
                                    @Param("currentOrder") int currentOrder);

    /**
     * Returns the previous lesson in the same module (for "Back" button).
     */
    @Query("""
        SELECT l FROM Lesson l
        WHERE l.module.id = :moduleId
          AND l.displayOrder < :currentOrder
          AND l.isDeleted = false
          AND l.isPublished = true
        ORDER BY l.displayOrder DESC
        LIMIT 1
        """)
    Optional<Lesson> findPreviousLesson(@Param("moduleId") UUID moduleId,
                                        @Param("currentOrder") int currentOrder);
}