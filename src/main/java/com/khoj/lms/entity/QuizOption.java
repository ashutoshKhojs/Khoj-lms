package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * A single answer option for a QuizQuestion.
 *
 * isCorrect is the source of truth for auto-evaluation.
 * Never exposed to the student before submission.
 *
 * Table: quiz_options
 */
@Entity
@Table(
        name = "quiz_options",
        indexes = {
                @Index(name = "idx_option_question", columnList = "question_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * TRUE = this option is correct.
     * NEVER send this field to the frontend during an active attempt.
     */
    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 1;
}