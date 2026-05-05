package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import com.khoj.lms.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Records a student's enrollment in a course.
 *
 * One row per (student, course) pair — unique constraint enforced.
 * Tracks overall progress %, completion timestamp, and last accessed lesson
 * to enable "Continue Learning" functionality.
 *
 * Table: enrollments
 */
@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_enrollment_student_course",
                        columnNames = {"student_id", "course_id"}
                )
        },
        indexes = {
                @Index(name = "idx_enrollment_student", columnList = "student_id"),
                @Index(name = "idx_enrollment_course",  columnList = "course_id"),
                @Index(name = "idx_enrollment_status",  columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    // ─────────────────────────────────────────
    // Progress Tracking
    // ─────────────────────────────────────────

    /** Overall course completion percentage (0–100) */
    @Column(name = "progress_percentage", nullable = false)
    @Builder.Default
    private Double progressPercentage = 0.0;

    @Column(name = "lessons_completed", nullable = false)
    @Builder.Default
    private Integer lessonsCompleted = 0;

    @Column(name = "total_time_spent_seconds", nullable = false)
    @Builder.Default
    private Long totalTimeSpentSeconds = 0L;

    // ─────────────────────────────────────────
    // Resume Learning — the "Continue" button
    // ─────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_accessed_lesson_id")
    private Lesson lastAccessedLesson;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    // ─────────────────────────────────────────
    // Completion
    // ─────────────────────────────────────────

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "is_certificate_issued", nullable = false)
    @Builder.Default
    private Boolean isCertificateIssued = false;

    // ─────────────────────────────────────────
    // Rating (student rates course after completion)
    // ─────────────────────────────────────────

    /** 1–5 star rating, null until student submits a review */
    @Column(name = "rating")
    private Integer rating;

    @Column(name = "review", columnDefinition = "TEXT")
    private String review;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // ─────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────

    public void markCompleted() {
        this.status = EnrollmentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.progressPercentage = 100.0;
    }

    public void updateLastAccessed(Lesson lesson) {
        this.lastAccessedLesson = lesson;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return EnrollmentStatus.COMPLETED.equals(this.status);
    }

    public boolean isActive() {
        return EnrollmentStatus.ACTIVE.equals(this.status);
    }
}