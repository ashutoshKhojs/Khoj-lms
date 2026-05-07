package com.khoj.lms.repository;

import com.khoj.lms.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModuleRepository extends JpaRepository<Module, UUID> {

    List<Module> findByCourseIdAndIsDeletedFalseOrderByDisplayOrderAsc(UUID courseId);

    Optional<Module> findByIdAndIsDeletedFalse(UUID id);

    @Query("SELECT COUNT(m) FROM Module m WHERE m.course.id = :courseId AND m.isDeleted = false")
    int countByCourseId(@Param("courseId") UUID courseId);

    @Modifying
    @Query("""
        UPDATE Module m
        SET m.totalLessons = :lessons,
            m.totalDurationSeconds = :duration
        WHERE m.id = :id
        """)
    void updateStats(@Param("id") UUID id,
                     @Param("lessons") int lessons,
                     @Param("duration") long duration);

    @Query("SELECT MAX(m.displayOrder) FROM Module m WHERE m.course.id = :courseId AND m.isDeleted = false")
    Integer findMaxDisplayOrder(@Param("courseId") UUID courseId);
}