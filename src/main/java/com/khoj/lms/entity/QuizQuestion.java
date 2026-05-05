package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import com.khoj.lms.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A single question inside a Quiz.
 *
 * Supports MCQ_SINGLE, MCQ_MULTIPLE, TRUE_FALSE (auto-graded)
 * and SHORT_TEXT (manually graded — Phase 2).
 *
 * Table: quiz_questions
 */
@Entity
@Table(
        name = "quiz_questions",
        indexes = {
                @Index(name = "idx_question_quiz", columnList = "quiz_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType;

    /** Points awarded for correct answer */
    @Column(name = "marks", nullable = false)
    @Builder.Default
    private Integer marks = 1;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 1;

    /** Explanation shown after submission */
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<QuizOption> options = new ArrayList<>();

    public boolean isAutoGradable() {
        return questionType != QuestionType.SHORT_TEXT;
    }
}