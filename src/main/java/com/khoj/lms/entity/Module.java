package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A module groups related lessons within a course.
 *
 * Hierarchy:  Course → Module → Lesson
 *
 * Example:
 *   Course: "AI Basics"
 *     Module 1: "Introduction to AI"
 *       Lesson 1.1: "What is AI?" (VIDEO)
 *       Lesson 1.2: "History of AI" (VIDEO)
 *       Lesson 1.3: "Module Quiz" (QUIZ)
 *     Module 2: "Machine Learning Basics"
 *       ...
 *
 * Table: modules
 */
@Entity
@Table(
        name = "modules",
        indexes = {
                @Index(name = "idx_module_course",  columnList = "course_id"),
                @Index(name = "idx_module_order",   columnList = "course_id, display_order")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Module extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @NotBlank
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Controls ordering within the course (1-based) */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 1;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    /** Prevents students from jumping ahead — must complete prior modules first */
    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    // ─────────────────────────────────────────
    // Stats (denormalized)
    // ─────────────────────────────────────────

    @Column(name = "total_lessons", nullable = false)
    @Builder.Default
    private Integer totalLessons = 0;

    @Column(name = "total_duration_seconds", nullable = false)
    @Builder.Default
    private Long totalDurationSeconds = 0L;

    // ─────────────────────────────────────────
    // Relations
    // ─────────────────────────────────────────

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<Lesson> lessons = new ArrayList<>();
}