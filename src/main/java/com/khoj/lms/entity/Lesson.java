package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import com.khoj.lms.enums.LessonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * A single unit of learning inside a module.
 *
 * Each lesson has a type (VIDEO, DOCUMENT, QUIZ, ASSIGNMENT, TEXT).
 * Video lessons carry S3 key + duration.
 * Quiz/Assignment lessons link via quizId / assignmentId.
 *
 * Table: lessons
 */
@Entity
@Table(
        name = "lessons",
        indexes = {
                @Index(name = "idx_lesson_module",  columnList = "module_id"),
                @Index(name = "idx_lesson_course",  columnList = "course_id"),
                @Index(name = "idx_lesson_order",   columnList = "module_id, display_order")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    /**
     * Denormalized course_id — allows direct lesson→course queries
     * without joining through module every time.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @NotBlank
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, length = 20)
    private LessonType lessonType;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 1;

    // ─────────────────────────────────────────
    // Video Fields (when lessonType = VIDEO)
    // ─────────────────────────────────────────

    /**
     * S3 object key — NOT the full URL.
     * Pre-signed URLs are generated on-demand to prevent link sharing.
     * Example: "courses/ai-basics/module-1/lesson-1.mp4"
     */
    @Column(name = "video_s3_key", length = 500)
    private String videoS3Key;

    /** Duration in seconds — stored for progress calculation */
    @Column(name = "video_duration_seconds")
    private Long videoDurationSeconds;

    /** Thumbnail image S3 key for the video */
    @Column(name = "video_thumbnail_s3_key", length = 500)
    private String videoThumbnailS3Key;

    // ─────────────────────────────────────────
    // Document / Notes Fields
    // ─────────────────────────────────────────

    /** S3 key for PDF notes attached to this lesson */
    @Column(name = "notes_s3_key", length = 500)
    private String notesS3Key;

    @Column(name = "notes_file_name", length = 255)
    private String notesFileName;

    // ─────────────────────────────────────────
    // Text Content (when lessonType = TEXT)
    // ─────────────────────────────────────────

    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    // ─────────────────────────────────────────
    // Quiz / Assignment Link
    // ─────────────────────────────────────────

    /** Populated when lessonType = QUIZ */
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    /** Populated when lessonType = ASSIGNMENT */
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    // ─────────────────────────────────────────
    // Access Control
    // ─────────────────────────────────────────

    /** If true, visible to non-enrolled users as a preview */
    @Column(name = "is_preview", nullable = false)
    @Builder.Default
    private Boolean isPreview = false;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    // ─────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────

    public boolean isVideoLesson() {
        return LessonType.VIDEO.equals(this.lessonType);
    }

    public boolean isQuizLesson() {
        return LessonType.QUIZ.equals(this.lessonType);
    }

    public boolean isAssignmentLesson() {
        return LessonType.ASSIGNMENT.equals(this.lessonType);
    }
}