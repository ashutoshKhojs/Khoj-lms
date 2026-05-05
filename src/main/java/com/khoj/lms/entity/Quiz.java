package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A quiz attached to a lesson (lessonType = QUIZ).
 *
 * Contains multiple QuizQuestion objects, each with options.
 * Supports auto-evaluation for MCQ_SINGLE, MCQ_MULTIPLE, TRUE_FALSE.
 *
 * Table: quizzes
 */
@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    /** Time limit in minutes. 0 = no limit */
    @Column(name = "time_limit_minutes", nullable = false)
    @Builder.Default
    private Integer timeLimitMinutes = 0;

    /** Passing score as percentage (0–100) */
    @Column(name = "passing_score", nullable = false)
    @Builder.Default
    private Integer passingScore = 60;

    /** How many times a student can attempt. 0 = unlimited */
    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 3;

    @Column(name = "shuffle_questions", nullable = false)
    @Builder.Default
    private Boolean shuffleQuestions = false;

    @Column(name = "shuffle_options", nullable = false)
    @Builder.Default
    private Boolean shuffleOptions = false;

    /** Show correct answers after submission */
    @Column(name = "show_answers_after_submit", nullable = false)
    @Builder.Default
    private Boolean showAnswersAfterSubmit = true;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<QuizQuestion> questions = new ArrayList<>();

    public int getTotalMarks() {
        return questions.stream().mapToInt(QuizQuestion::getMarks).sum();
    }
}