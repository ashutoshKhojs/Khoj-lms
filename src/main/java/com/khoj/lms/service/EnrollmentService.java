package com.khoj.lms.service;

import com.khoj.lms.dto.enrollment.*;
import com.khoj.lms.entity.Enrollment;
import com.khoj.lms.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface EnrollmentService {

    // ───── STUDENT ─────

    void unenroll(UUID courseId, String studentEmail);

    Page<EnrollmentSummary> getMyEnrollments(String studentEmail,
                                             EnrollmentStatus status,
                                             Pageable pageable);

    EnrollmentResponse getMyEnrollmentForCourse(UUID courseId, String studentEmail);

    List<EnrollmentSummary> getContinueLearning(String studentEmail, int limit);

    EnrollmentResponse reviewCourse(UUID courseId,
                                    CourseReviewRequest request,
                                    String studentEmail);

    // ───── INSTRUCTOR ─────
    Page<EnrollmentSummary> getCourseEnrollmentsForInstructor(UUID courseId,
                                                              String instructorEmail,
                                                              Pageable pageable);

    CourseEnrollmentStats getCourseStatsForInstructor(UUID courseId, String instructorEmail);

    // ───── ADMIN ─────
    Page<EnrollmentSummary> adminListAll(EnrollmentStatus status, Pageable pageable);

    Page<EnrollmentSummary> adminListByCourse(UUID courseId, Pageable pageable);

    Page<EnrollmentSummary> adminListByStudent(UUID studentId, Pageable pageable);

    CourseEnrollmentStats adminGetCourseStats(UUID courseId);

    // ───── INTERNAL (called by LessonProgressService) ─────
    /** Updates progress %, lessonsCompleted, and auto-marks COMPLETED at threshold. */
    void recalculateProgress(UUID enrollmentId);

    Enrollment findOrThrow(UUID id);

    Enrollment findByStudentAndCourseOrThrow(UUID studentId, UUID courseId);
}