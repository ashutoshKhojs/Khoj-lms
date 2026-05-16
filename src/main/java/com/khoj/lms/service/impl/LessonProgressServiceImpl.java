package com.khoj.lms.service.impl;

import com.khoj.lms.audit.AuditLogger;
import com.khoj.lms.dto.progress.*;
import com.khoj.lms.entity.*;
import com.khoj.lms.enums.LessonType;
import com.khoj.lms.exception.*;
import com.khoj.lms.repository.*;
import com.khoj.lms.service.EnrollmentService;
import com.khoj.lms.service.LessonProgressService;
import com.khoj.lms.service.LessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class LessonProgressServiceImpl implements LessonProgressService {

    /** Watch percentage at which a video lesson is auto-completed. */
    private static final double AUTO_COMPLETE_THRESHOLD = 90.0;

    private final LessonProgressRepository progressRepository;
    private final LessonService            lessonService;
    private final EnrollmentService        enrollmentService;
    private final EnrollmentRepository     enrollmentRepository;
    private final UserRepository           userRepository;
    private final AuditLogger              auditLogger;

    // ═══════════════════════════════════════════════════════════
    // STUDENT
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public LessonProgressResponse recordAccess(UUID lessonId, String studentEmail) {
        log.debug("Record access: student={} lessonId={}", studentEmail, lessonId);

        Context ctx = loadContext(lessonId, studentEmail);

        LessonProgress progress = progressRepository
                .findByStudentAndLesson(ctx.student.getId(), lessonId)
                .orElseGet(() -> createNewProgress(ctx));

        progress.recordAccess();
        progress = progressRepository.save(progress);

        // Update enrollment's last accessed lesson (powers "Continue Learning")
        ctx.enrollment.updateLastAccessed(ctx.lesson);
        enrollmentRepository.save(ctx.enrollment);

        log.debug("Access recorded: progressId={} accessCount={}",
                progress.getId(), progress.getAccessCount());

        return toResponse(progress);
    }

    @Override
    @Transactional
    public LessonProgressResponse updateWatchPosition(UUID lessonId,
                                                      WatchPositionRequest request,
                                                      String studentEmail) {

        log.debug("Watch position: student={} lessonId={} position={}s",
                studentEmail, lessonId, request.getWatchPositionSeconds());

        Context ctx = loadContext(lessonId, studentEmail);

        if (ctx.lesson.getLessonType() != LessonType.VIDEO) {
            throw new BadRequestException("Watch position only applies to VIDEO lessons.");
        }

        LessonProgress progress = progressRepository
                .findByStudentAndLesson(ctx.student.getId(), lessonId)
                .orElseGet(() -> createNewProgress(ctx));

        // Update position + watch percentage
        progress.updateWatchPosition(
                request.getWatchPositionSeconds(),
                ctx.lesson.getVideoDurationSeconds()
        );

        // Accumulate watch time (caller sends delta — clamp to avoid abuse)
        if (request.getDeltaSeconds() != null && request.getDeltaSeconds() > 0) {
            long delta = Math.min(request.getDeltaSeconds(), 60L); // max 60s per heartbeat
            long current = progress.getTotalWatchTimeSeconds() != null
                    ? progress.getTotalWatchTimeSeconds() : 0L;
            progress.setTotalWatchTimeSeconds(current + delta);
        }

        boolean wasCompleted = Boolean.TRUE.equals(progress.getIsCompleted());

        // Auto-complete at threshold
        if (!wasCompleted
                && progress.getWatchPercentage() != null
                && progress.getWatchPercentage() >= AUTO_COMPLETE_THRESHOLD) {

            progress.markCompleted();
            log.info("Lesson auto-completed at {}%: student={} lessonId={}",
                    AUTO_COMPLETE_THRESHOLD, studentEmail, lessonId);
        }

        progress = progressRepository.save(progress);

        // If state changed to completed → bump enrollment counters
        if (!wasCompleted && Boolean.TRUE.equals(progress.getIsCompleted())) {
            applyLessonCompletion(ctx.enrollment);
        }

        return toResponse(progress);
    }

    @Override
    @Transactional
    public LessonProgressResponse markCompleted(UUID lessonId, String studentEmail) {
        log.info("Manual mark complete: student={} lessonId={}", studentEmail, lessonId);

        Context ctx = loadContext(lessonId, studentEmail);

        LessonProgress progress = progressRepository
                .findByStudentAndLesson(ctx.student.getId(), lessonId)
                .orElseGet(() -> createNewProgress(ctx));

        if (Boolean.TRUE.equals(progress.getIsCompleted())) {
            log.debug("Lesson already completed — no-op: lessonId={}", lessonId);
            return toResponse(progress);
        }

        progress.markCompleted();
        progress = progressRepository.save(progress);

        applyLessonCompletion(ctx.enrollment);

        log.info("Lesson manually completed: student={} lessonId={} lessonTitle='{}'",
                studentEmail, lessonId, ctx.lesson.getTitle());

        return toResponse(progress);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonProgressResponse getMyLessonProgress(UUID lessonId, String studentEmail) {
        Context ctx = loadContext(lessonId, studentEmail);

        return progressRepository
                .findByStudentAndLesson(ctx.student.getId(), lessonId)
                .map(this::toResponse)
                .orElseGet(() -> emptyResponseFor(ctx));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseProgressSummary getMyCourseProgress(UUID courseId, String studentEmail) {
        User student = findUserByEmail(studentEmail);
        Enrollment enrollment = enrollmentService.findByStudentAndCourseOrThrow(
                student.getId(), courseId);

        List<LessonProgress> all = progressRepository.findByEnrollment(enrollment.getId());

        return CourseProgressSummary.builder()
                .courseId(courseId)
                .enrollmentId(enrollment.getId())
                .totalLessons(enrollment.getCourse().getTotalLessons())
                .lessonsCompleted(enrollment.getLessonsCompleted())
                .progressPercentage(enrollment.getProgressPercentage())
                .totalWatchTimeSeconds(enrollment.getTotalTimeSpentSeconds())
                .lessonProgress(all.stream().map(this::toResponse).toList())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // ADMIN
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public CourseProgressSummary adminGetCourseProgress(UUID courseId, UUID studentId) {
        Enrollment enrollment = enrollmentService
                .findByStudentAndCourseOrThrow(studentId, courseId);

        List<LessonProgress> all = progressRepository.findByEnrollment(enrollment.getId());

        log.debug("[ADMIN] Fetched course progress: courseId={} studentId={}",
                courseId, studentId);

        return CourseProgressSummary.builder()
                .courseId(courseId)
                .enrollmentId(enrollment.getId())
                .totalLessons(enrollment.getCourse().getTotalLessons())
                .lessonsCompleted(enrollment.getLessonsCompleted())
                .progressPercentage(enrollment.getProgressPercentage())
                .totalWatchTimeSeconds(enrollment.getTotalTimeSpentSeconds())
                .lessonProgress(all.stream().map(this::toResponse).toList())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    /** Bundle of objects every endpoint needs — keeps each method clean. */
    private record Context(User student, Lesson lesson, Enrollment enrollment) {}

    private Context loadContext(UUID lessonId, String studentEmail) {
        User student = findUserByEmail(studentEmail);
        Lesson lesson = lessonService.findOrThrow(lessonId);
        Enrollment enrollment = enrollmentService.findByStudentAndCourseOrThrow(
                student.getId(), lesson.getCourse().getId());
        return new Context(student, lesson, enrollment);
    }

    private LessonProgress createNewProgress(Context ctx) {
        log.debug("Creating fresh LessonProgress: student={} lessonId={}",
                ctx.student.getEmail(), ctx.lesson.getId());
        return LessonProgress.builder()
                .student(ctx.student)
                .lesson(ctx.lesson)
                .enrollment(ctx.enrollment)
                .isCompleted(false)
                .watchPositionSeconds(0L)
                .totalWatchTimeSeconds(0L)
                .watchPercentage(0.0)
                .accessCount(0)
                .build();
    }

    /**
     * Centralized side-effect for a lesson transitioning to COMPLETED:
     *   - increment enrollment.lessonsCompleted
     *   - accumulate enrollment.totalTimeSpentSeconds
     *   - delegate progress % + auto-complete to EnrollmentService
     */
    private void applyLessonCompletion(Enrollment enrollment) {
        int current = enrollment.getLessonsCompleted() != null
                ? enrollment.getLessonsCompleted() : 0;
        enrollment.setLessonsCompleted(current + 1);

        long totalWatch = progressRepository.sumWatchTimeByEnrollment(enrollment.getId());
        enrollment.setTotalTimeSpentSeconds(totalWatch);

        enrollmentRepository.save(enrollment);

        // Recalc % and trigger COMPLETED transition if all done
        enrollmentService.recalculateProgress(enrollment.getId());

        auditLogger.adminAction(
                enrollment.getStudent().getEmail(),
                "LESSON_COMPLETED in: " + enrollment.getCourse().getTitle(),
                null
        );
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private LessonProgressResponse emptyResponseFor(Context ctx) {
        return LessonProgressResponse.builder()
                .lessonId(ctx.lesson.getId())
                .lessonTitle(ctx.lesson.getTitle())
                .courseId(ctx.lesson.getCourse().getId())
                .enrollmentId(ctx.enrollment.getId())
                .isCompleted(false)
                .watchPositionSeconds(0L)
                .totalWatchTimeSeconds(0L)
                .watchPercentage(0.0)
                .accessCount(0)
                .build();
    }

    private LessonProgressResponse toResponse(LessonProgress lp) {
        Double pct = lp.getWatchPercentage();
        if (pct != null) {
            pct = BigDecimal.valueOf(pct).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        return LessonProgressResponse.builder()
                .id(lp.getId())
                .lessonId(lp.getLesson().getId())
                .lessonTitle(lp.getLesson().getTitle())
                .courseId(lp.getLesson().getCourse().getId())
                .enrollmentId(lp.getEnrollment().getId())
                .isCompleted(lp.getIsCompleted())
                .completedAt(lp.getCompletedAt())
                .watchPositionSeconds(lp.getWatchPositionSeconds())
                .totalWatchTimeSeconds(lp.getTotalWatchTimeSeconds())
                .watchPercentage(pct)
                .firstAccessedAt(lp.getFirstAccessedAt())
                .lastAccessedAt(lp.getLastAccessedAt())
                .accessCount(lp.getAccessCount())
                .build();
    }
}