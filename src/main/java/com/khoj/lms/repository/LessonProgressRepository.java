package com.khoj.lms.repository;

import com.khoj.lms.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    Optional<LessonProgress> findByIdAndIsDeletedFalse(UUID id);

    @Query("""
           SELECT lp FROM LessonProgress lp
           WHERE lp.student.id = :studentId
             AND lp.lesson.id  = :lessonId
             AND lp.isDeleted  = false
           """)
    Optional<LessonProgress> findByStudentAndLesson(@Param("studentId") UUID studentId,
                                                    @Param("lessonId")  UUID lessonId);

    @Query("""
           SELECT lp FROM LessonProgress lp
           WHERE lp.enrollment.id = :enrollmentId
             AND lp.isDeleted = false
           ORDER BY lp.lesson.displayOrder ASC
           """)
    List<LessonProgress> findByEnrollment(@Param("enrollmentId") UUID enrollmentId);

    @Query("""
           SELECT lp FROM LessonProgress lp
           WHERE lp.student.id = :studentId
             AND lp.lesson.course.id = :courseId
             AND lp.isDeleted = false
           ORDER BY lp.lesson.displayOrder ASC
           """)
    List<LessonProgress> findByStudentAndCourse(@Param("studentId") UUID studentId,
                                                @Param("courseId")  UUID courseId);

    @Query("""
           SELECT COUNT(lp) FROM LessonProgress lp
           WHERE lp.enrollment.id = :enrollmentId
             AND lp.isCompleted = true
             AND lp.isDeleted   = false
           """)
    int countCompletedByEnrollment(@Param("enrollmentId") UUID enrollmentId);

    @Query("""
           SELECT COALESCE(SUM(lp.totalWatchTimeSeconds), 0)
           FROM LessonProgress lp
           WHERE lp.enrollment.id = :enrollmentId
             AND lp.isDeleted = false
           """)
    long sumWatchTimeByEnrollment(@Param("enrollmentId") UUID enrollmentId);
}