package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Records a student's answer to one question within a QuizAttempt.
 *
 * For MCQ: selected_option_ids stores a JSON array of chosen option IDs.
 * For TRUE_FALSE: stores "true" or "false" in text_answer.
 * For SHORT_TEXT: stores typed answer in text_answer.
 *
 * Table: quiz_answers
 */
@Entity
@Table(
        name = "quiz_answers",
        indexes = {
                @Index(name = "idx_answer_attempt",  columnList = "attempt_id"),
                @Index(name = "idx_answer_question", columnList = "question_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAnswer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    /**
     * JSON array of selected option IDs.
     * Single: ["uuid-1"]
     * Multiple: ["uuid-1","uuid-3"]
     */
    @Column(name = "selected_option_ids", columnDefinition = "TEXT")
    private String selectedOptionIds;

    /** Used for TRUE_FALSE and SHORT_TEXT answers */
    @Column(name = "text_answer", columnDefinition = "TEXT")
    private String textAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "marks_awarded")
    @Builder.Default
    private Integer marksAwarded = 0;
}