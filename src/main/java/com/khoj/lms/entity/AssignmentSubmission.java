package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A student's submission for an assignment.
 *
 * Can contain text, a file (S3 key), or both.
 * Instructor grades it by setting marks_awarded + feedback.
 *
 * Table: assignment_submissions
 */
@Entity
@Table(
        name = "assignment_submissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_submission_student_assignment",
                        columnNames = {"student_id", "assignment_id"}
                )
        },
        indexes = {
                @Index(name = "idx_submission_student",    columnList = "student_id"),
                @Index(name = "idx_submission_assignment", columnList = "assignment_id"),
                @Index(name = "idx_submission_graded",     columnList = "is_graded")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSubmission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Column(name = "text_submission", columnDefinition = "TEXT")
    private String textSubmission;

    /** S3 object key for the uploaded file */
    @Column(name = "file_s3_key", length = 500)
    private String fileS3Key;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "submitted_at", nullable = false)
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();

    // ─────────────────────────────────────────
    // Grading
    // ─────────────────────────────────────────

    @Column(name = "is_graded", nullable = false)
    @Builder.Default
    private Boolean isGraded = false;

    @Column(name = "marks_awarded")
    private Integer marksAwarded;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;

    // ─────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────

    public void grade(int marks, String feedback, User instructor) {
        this.marksAwarded = marks;
        this.feedback = feedback;
        this.gradedBy = instructor;
        this.gradedAt = LocalDateTime.now();
        this.isGraded = true;
    }

    public boolean isPassed(int passingMarks) {
        return this.marksAwarded != null && this.marksAwarded >= passingMarks;
    }
}