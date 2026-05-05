package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks a student's interaction with a single lesson.
 *
 * One row per (student, lesson) pair.
 * For video lessons: stores watch position for resume.
 * Completion of all lessons in a course triggers certificate eligibility.
 *
 * Table: lesson_progress
 */
@Entity
@Table(
        name = "lesson_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_lesson_progress_student_lesson",
                        columnNames = {"student_id", "lesson_id"}
                )
        },
        indexes = {
                @Index(name = "idx_lp_student",     columnList = "student_id"),
                @Index(name = "idx_lp_lesson",      columnList = "lesson_id"),
                @Index(name = "idx_lp_enrollment",  columnList = "enrollment_id"),
                @Index(name = "idx_lp_completed",   columnList = "is_completed")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonProgress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    // ─────────────────────────────────────────
    // Completion State
    // ─────────────────────────────────────────

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // ─────────────────────────────────────────
    // Video Resume (for VIDEO lessons)
    // ─────────────────────────────────────────

    /**
     * Last watched position in seconds.
     * Enables "Resume from where you left off" for video lessons.
     */
    @Column(name = "watch_position_seconds")
    @Builder.Default
    private Long watchPositionSeconds = 0L;

    /** Total seconds the student has watched (across all sessions) */
    @Column(name = "total_watch_time_seconds")
    @Builder.Default
    private Long totalWatchTimeSeconds = 0L;

    /**
     * Watch percentage (0–100).
     * Auto-marks completed when this reaches the threshold (e.g. 90%).
     */
    @Column(name = "watch_percentage")
    @Builder.Default
    private Double watchPercentage = 0.0;

    // ─────────────────────────────────────────
    // Access Tracking
    // ─────────────────────────────────────────

    @Column(name = "first_accessed_at")
    private LocalDateTime firstAccessedAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "access_count", nullable = false)
    @Builder.Default
    private Integer accessCount = 0;

    // ─────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────

    public void markCompleted() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
        this.watchPercentage = 100.0;
    }

    public void recordAccess() {
        if (this.firstAccessedAt == null) {
            this.firstAccessedAt = LocalDateTime.now();
        }
        this.lastAccessedAt = LocalDateTime.now();
        this.accessCount++;
    }

    public void updateWatchPosition(Long positionSeconds, Long totalDurationSeconds) {
        this.watchPositionSeconds = positionSeconds;
        if (totalDurationSeconds != null && totalDurationSeconds > 0) {
            this.watchPercentage = (positionSeconds * 100.0) / totalDurationSeconds;
        }
    }
}