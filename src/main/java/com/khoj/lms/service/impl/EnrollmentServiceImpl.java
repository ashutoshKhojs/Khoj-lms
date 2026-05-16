package com.khoj.lms.service.impl;

import com.khoj.lms.audit.AuditLogger;
import com.khoj.lms.dto.enrollment.*;
import com.khoj.lms.entity.*;
import com.khoj.lms.enums.CourseStatus;
import com.khoj.lms.enums.EnrollmentStatus;
import com.khoj.lms.exception.*;
import com.khoj.lms.repository.*;
import com.khoj.lms.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository    enrollmentRepository;
    private final CourseRepository        courseRepository;
    private final UserRepository          userRepository;
    private final LessonRepository        lessonRepository;
    private final AuditLogger             auditLogger;

    // ═══════════════════════════════════════════════════════════
    // STUDENT
    // ═══════════════════════════════════════════════════════════



    @Override
    @Transactional
    public void unenroll(UUID courseId, String studentEmail) {
        log.info("Unenroll request: student={} courseId={}", studentEmail, courseId);

        User student = findUserByEmail(studentEmail);
        Enrollment enrollment = findByStudentAndCourseOrThrow(student.getId(), courseId);

        // Block unenroll if any progress has been made
        if (enrollment.getLessonsCompleted() != null && enrollment.getLessonsCompleted() > 0) {
            log.warn("Unenroll denied — progress exists: enrollmentId={} lessonsCompleted={}",
                    enrollment.getId(), enrollment.getLessonsCompleted());
            throw new BadRequestException(
                    "Cannot unenroll after starting the course. Contact support if needed.");
        }

        if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            throw new BadRequestException("Cannot unenroll from a completed course.");
        }

        enrollment.softDelete();
        enrollmentRepository.save(enrollment);

        // Decrement course counter (don't go below 0)
        Course course = enrollment.getCourse();
        if (course.getEnrolledCount() != null && course.getEnrolledCount() > 0) {
            course.setEnrolledCount(course.getEnrolledCount() - 1);
            courseRepository.save(course);
        }

        log.info("Enrollment soft-deleted: id={} student={} courseId={}",
                enrollment.getId(), studentEmail, courseId);

        auditLogger.adminAction(
                studentEmail,
                "COURSE_UNENROLLED: " + course.getTitle(),
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentSummary> getMyEnrollments(String studentEmail,
                                                    EnrollmentStatus status,
                                                    Pageable pageable) {
        User student = findUserByEmail(studentEmail);
        log.debug("Fetching enrollments for student={} status={}", studentEmail, status);

        return enrollmentRepository
                .findMyEnrollments(student.getId(), status, pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponse getMyEnrollmentForCourse(UUID courseId, String studentEmail) {
        User student = findUserByEmail(studentEmail);
        Enrollment enrollment = findByStudentAndCourseOrThrow(student.getId(), courseId);
        return toResponse(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentSummary> getContinueLearning(String studentEmail, int limit) {
        User student = findUserByEmail(studentEmail);
        int cappedLimit = Math.max(1, Math.min(limit, 10));

        return enrollmentRepository
                .findContinueLearning(student.getId(), PageRequest.of(0, cappedLimit))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public EnrollmentResponse reviewCourse(UUID courseId,
                                           CourseReviewRequest request,
                                           String studentEmail) {

        log.info("Review request: student={} courseId={} rating={}",
                studentEmail, courseId, request.getRating());

        User student = findUserByEmail(studentEmail);
        Enrollment enrollment = findByStudentAndCourseOrThrow(student.getId(), courseId);

        if (enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
            throw new BadRequestException("You can only review a course after completing it.");
        }

        boolean isUpdate = enrollment.getRating() != null;
        Integer oldRating = enrollment.getRating();

        enrollment.setRating(request.getRating());
        enrollment.setReview(request.getReview());
        enrollment.setReviewedAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        // Recompute course's averageRating + ratingCount
        recalculateCourseRating(enrollment.getCourse(), isUpdate, oldRating, request.getRating());

        log.info("Course reviewed: enrollmentId={} student={} rating={} isUpdate={}",
                enrollment.getId(), studentEmail, request.getRating(), isUpdate);

        auditLogger.adminAction(
                studentEmail,
                "COURSE_REVIEWED: " + enrollment.getCourse().getTitle()
                        + " rating=" + request.getRating(),
                null
        );

        return toResponse(enrollment);
    }

    // ═══════════════════════════════════════════════════════════
    // INSTRUCTOR
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentSummary> getCourseEnrollmentsForInstructor(UUID courseId,
                                                                     String instructorEmail,
                                                                     Pageable pageable) {
        Course course = findCourseOrThrow(courseId);
        assertOwns(course, instructorEmail);

        log.debug("[INSTRUCTOR] Fetching enrollments: courseId={} by={}",
                courseId, instructorEmail);

        return enrollmentRepository.findByCourse(courseId, pageable).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseEnrollmentStats getCourseStatsForInstructor(UUID courseId,
                                                             String instructorEmail) {
        Course course = findCourseOrThrow(courseId);
        assertOwns(course, instructorEmail);
        return buildStats(course);
    }

    // ═══════════════════════════════════════════════════════════
    // ADMIN
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentSummary> adminListAll(EnrollmentStatus status, Pageable pageable) {
        log.debug("[ADMIN] Listing all enrollments status={}", status);
        return enrollmentRepository.findAllForAdmin(status, pageable).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentSummary> adminListByCourse(UUID courseId, Pageable pageable) {
        findCourseOrThrow(courseId);
        return enrollmentRepository.findByCourse(courseId, pageable).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentSummary> adminListByStudent(UUID studentId, Pageable pageable) {
        return enrollmentRepository.findByStudentForAdmin(studentId, pageable).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseEnrollmentStats adminGetCourseStats(UUID courseId) {
        Course course = findCourseOrThrow(courseId);
        return buildStats(course);
    }

    // ═══════════════════════════════════════════════════════════
    // INTERNAL
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void recalculateProgress(UUID enrollmentId) {
        Enrollment enrollment = findOrThrow(enrollmentId);
        Course course = enrollment.getCourse();

        int totalLessons = course.getTotalLessons() != null ? course.getTotalLessons() : 0;
        int completed   = enrollment.getLessonsCompleted() != null ? enrollment.getLessonsCompleted() : 0;

        double pct = totalLessons == 0 ? 0.0
                : Math.min(100.0, (completed * 100.0) / totalLessons);

        enrollment.setProgressPercentage(round2(pct));
        enrollmentRepository.save(enrollment);

        // Auto-complete enrollment when all lessons done
        if (totalLessons > 0 && completed >= totalLessons
                && enrollment.getStatus() == EnrollmentStatus.ACTIVE) {

            enrollment.markCompleted();
            enrollmentRepository.save(enrollment);

            courseRepository.incrementCompletionCount(course.getId());

            log.info("Enrollment auto-completed: id={} student={} courseId={}",
                    enrollmentId,
                    enrollment.getStudent().getEmail(),
                    course.getId());

            auditLogger.adminAction(
                    enrollment.getStudent().getEmail(),
                    "COURSE_COMPLETED: " + course.getTitle(),
                    null
            );

            // TODO Wk 10: trigger certificate generation here
        }

        log.debug("Progress recalculated: enrollmentId={} completed={} totalLessons={} pct={}",
                enrollmentId, completed, totalLessons, pct);
    }

    @Override
    public Enrollment findOrThrow(UUID id) {
        return enrollmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", id));
    }

    @Override
    public Enrollment findByStudentAndCourseOrThrow(UUID studentId, UUID courseId) {
        return enrollmentRepository.findByStudentAndCourse(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment", "courseId", courseId));
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private User findUserByEmail(String email) {
        return userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Course findCourseOrThrow(UUID id) {
        return courseRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));
    }

    private void assertOwns(Course course, String email) {
        if (!course.getInstructor().getEmail().equalsIgnoreCase(email)) {
            log.warn("Ownership check failed — email={} does not own courseId={}",
                    email, course.getId());
            throw new AccessDeniedException("You do not own this course.");
        }
    }

    private CourseEnrollmentStats buildStats(Course course) {
        long total      = enrollmentRepository.countByCourse(course.getId());
        long completed  = enrollmentRepository.countCompletedByCourse(course.getId());
        long active     = Math.max(0, total - completed);
        double avgProg  = enrollmentRepository.avgProgressByCourse(course.getId());
        double compRate = total == 0 ? 0.0 : (completed * 100.0) / total;

        return CourseEnrollmentStats.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .totalEnrollments(total)
                .activeEnrollments(active)
                .completedEnrollments(completed)
                .averageProgress(round2(avgProg))
                .completionRate(round2(compRate))
                .build();
    }

    private void recalculateCourseRating(Course course, boolean isUpdate,
                                         Integer oldRating, Integer newRating) {
        BigDecimal currentSum = course.getAverageRating() != null
                ? course.getAverageRating().multiply(BigDecimal.valueOf(course.getRatingCount()))
                : BigDecimal.ZERO;

        BigDecimal newSum;
        int newCount;

        if (isUpdate && oldRating != null) {
            // Replace the old rating with the new one
            newSum   = currentSum.subtract(BigDecimal.valueOf(oldRating))
                    .add(BigDecimal.valueOf(newRating));
            newCount = course.getRatingCount();
        } else {
            newSum   = currentSum.add(BigDecimal.valueOf(newRating));
            newCount = course.getRatingCount() + 1;
        }

        BigDecimal newAvg = newCount == 0
                ? BigDecimal.ZERO
                : newSum.divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);

        course.setAverageRating(newAvg);
        course.setRatingCount(newCount);
        courseRepository.save(course);

        log.debug("Course rating updated: courseId={} avg={} count={}",
                course.getId(), newAvg, newCount);
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    // ═══════════════════════════════════════════════════════════
    // MAPPERS
    // ═══════════════════════════════════════════════════════════

    private EnrollmentResponse toResponse(Enrollment e) {
        Course  c = e.getCourse();
        User    s = e.getStudent();
        Lesson  last = e.getLastAccessedLesson();

        return EnrollmentResponse.builder()
                .id(e.getId())
                .courseId(c.getId())
                .courseSlug(c.getSlug())
                .courseTitle(c.getTitle())
                .courseThumbnailUrl(c.getThumbnailUrl())
                .instructorName(c.getInstructor().getFullName())
                .totalLessons(c.getTotalLessons())
                .totalDurationSeconds(c.getTotalDurationSeconds())
                .studentId(s.getId())
                .studentName(s.getFullName())
                .studentEmail(s.getEmail())
                .status(e.getStatus())
                .progressPercentage(e.getProgressPercentage())
                .lessonsCompleted(e.getLessonsCompleted())
                .totalTimeSpentSeconds(e.getTotalTimeSpentSeconds())
                .lastAccessedLessonId(last != null ? last.getId() : null)
                .lastAccessedLessonTitle(last != null ? last.getTitle() : null)
                .lastAccessedAt(e.getLastAccessedAt())
                .completedAt(e.getCompletedAt())
                .isCertificateIssued(e.getIsCertificateIssued())
                .rating(e.getRating())
                .review(e.getReview())
                .reviewedAt(e.getReviewedAt())
                .enrolledAt(e.getCreatedAt())
                .build();
    }

    private EnrollmentSummary toSummary(Enrollment e) {
        Course c = e.getCourse();
        Lesson last = e.getLastAccessedLesson();
        return EnrollmentSummary.builder()
                .id(e.getId())
                .courseId(c.getId())
                .courseTitle(c.getTitle())
                .courseSlug(c.getSlug())
                .courseThumbnailUrl(c.getThumbnailUrl())
                .instructorName(c.getInstructor().getFullName())
                .status(e.getStatus())
                .progressPercentage(e.getProgressPercentage())
                .lessonsCompleted(e.getLessonsCompleted())
                .totalLessons(c.getTotalLessons())
                .lastAccessedLessonId(last != null ? last.getId() : null)
                .lastAccessedAt(e.getLastAccessedAt())
                .enrolledAt(e.getCreatedAt())
                .build();
    }
}