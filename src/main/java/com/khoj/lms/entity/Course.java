package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import com.khoj.lms.enums.CourseStatus;
import com.khoj.lms.enums.DifficultyLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A course on the Khoj LMS platform.
 *
 * Hierarchy:  Course → Module → Lesson
 *
 * Lifecycle:
 *   Instructor creates (DRAFT) → submits for review (PENDING)
 *   → Admin approves (PUBLISHED) or rejects (REJECTED)
 *   → Can be archived later (ARCHIVED)
 *
 * Table: courses
 */
@Entity
@Table(
        name = "courses",
        indexes = {
                @Index(name = "idx_course_slug",       columnList = "slug"),
                @Index(name = "idx_course_status",     columnList = "status"),
                @Index(name = "idx_course_instructor", columnList = "instructor_id"),
                @Index(name = "idx_course_category",   columnList = "category_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course extends BaseEntity {

    // ─────────────────────────────────────────
    // Core Information
    // ─────────────────────────────────────────

    @NotBlank
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** URL-safe slug — e.g. "ai-basics-for-beginners" */
    @NotBlank
    @Column(name = "slug", nullable = false, unique = true, length = 220)
    private String slug;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "what_you_will_learn", columnDefinition = "TEXT")
    private String whatYouWillLearn;      // Stored as JSON array of strings

    @Column(name = "prerequisites", columnDefinition = "TEXT")
    private String prerequisites;         // JSON array

    @Column(name = "target_audience", columnDefinition = "TEXT")
    private String targetAudience;

    // ─────────────────────────────────────────
    // Media
    // ─────────────────────────────────────────

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "preview_video_url", length = 500)
    private String previewVideoUrl;       // Free preview (S3 key)

    // ─────────────────────────────────────────
    // Classification
    // ─────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false, length = 20)
    @Builder.Default
    private DifficultyLevel difficultyLevel = DifficultyLevel.BEGINNER;

    @Column(name = "language", length = 50)
    @Builder.Default
    private String language = "English";

    @Column(name = "tags", length = 500)
    private String tags;                  // Comma-separated e.g. "python,ai,beginner"

    // ─────────────────────────────────────────
    // Ownership
    // ─────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    // ─────────────────────────────────────────
    // Pricing (Khoj is free — but field is ready for Phase 2)
    // ─────────────────────────────────────────

    @Column(name = "is_free", nullable = false)
    @Builder.Default
    private Boolean isFree = true;

    @Column(name = "price", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    // ─────────────────────────────────────────
    // Status & Approval
    // ─────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    // ─────────────────────────────────────────
    // Stats (denormalized for fast reads)
    // ─────────────────────────────────────────

    @Column(name = "total_modules", nullable = false)
    @Builder.Default
    private Integer totalModules = 0;

    @Column(name = "total_lessons", nullable = false)
    @Builder.Default
    private Integer totalLessons = 0;

    @Column(name = "total_duration_seconds", nullable = false)
    @Builder.Default
    private Long totalDurationSeconds = 0L;

    @Column(name = "enrolled_count", nullable = false)
    @Builder.Default
    private Integer enrolledCount = 0;

    @Column(name = "completion_count", nullable = false)
    @Builder.Default
    private Integer completionCount = 0;

    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    private Integer ratingCount = 0;

    // ─────────────────────────────────────────
    // Certificate
    // ─────────────────────────────────────────

    @Column(name = "has_certificate", nullable = false)
    @Builder.Default
    private Boolean hasCertificate = true;

    /** 0–100. Student must reach this % to get the certificate */
    @Column(name = "certificate_threshold", nullable = false)
    @Builder.Default
    private Integer certificateThreshold = 80;

    // ─────────────────────────────────────────
    // Relations
    // ─────────────────────────────────────────

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
            orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<Module> modules = new ArrayList<>();

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Enrollment> enrollments = new ArrayList<>();

    // ─────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────

    public void publish() {
        this.status = CourseStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.status = CourseStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void archive() {
        this.status = CourseStatus.ARCHIVED;
    }

    public void incrementEnrolledCount() {
        this.enrolledCount++;
    }

    public void incrementCompletionCount() {
        this.completionCount++;
    }

    public boolean isPublished() {
        return CourseStatus.PUBLISHED.equals(this.status);
    }
}