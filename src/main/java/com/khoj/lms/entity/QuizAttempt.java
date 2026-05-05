package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import com.khoj.lms.enums.AttemptStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Records one attempt by a student on a quiz.
 *
 * On submission: score is calculated, status set to PASSED/FAILED.
 * Each submitted answer is stored in QuizAnswer for audit trail.
 *
 * Table: quiz_attempts
 */
@Entity
@Table(
        name = "quiz_attempts",
        indexes = {
                @Index(name = "idx_attempt_student", columnList = "student_id"),
                @Index(name = "idx_attempt_quiz",    columnList = "quiz_id"),
                @Index(name = "idx_attempt_status",  columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private Integer attemptNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    // ─────────────────────────────────────────
    // Timing
    // ─────────────────────────────────────────

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "time_taken_seconds")
    private Long timeTakenSeconds;

    // ─────────────────────────────────────────
    // Score
    // ─────────────────────────────────────────

    @Column(name = "score_obtained")
    @Builder.Default
    private Integer scoreObtained = 0;

    @Column(name = "total_marks")
    private Integer totalMarks;

    @Column(name = "percentage")
    private Double percentage;

    @Column(name = "is_passed", nullable = false)
    @Builder.Default
    private Boolean isPassed = false;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<QuizAnswer> answers = new ArrayList<>();

    // ─────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────

    public void submit(int score, int total, int passingScore) {
        this.scoreObtained = score;
        this.totalMarks = total;
        this.percentage = total > 0 ? (score * 100.0) / total : 0.0;
        this.isPassed = this.percentage >= passingScore;
        this.status = this.isPassed ? AttemptStatus.PASSED : AttemptStatus.FAILED;
        this.submittedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.timeTakenSeconds = java.time.Duration
                    .between(this.startedAt, this.submittedAt).getSeconds();
        }
    }
}