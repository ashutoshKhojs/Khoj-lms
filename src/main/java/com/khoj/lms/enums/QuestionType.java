package com.khoj.lms.enums;

/**
 * Type of a quiz question.
 *
 * MCQ_SINGLE   → Single correct answer from options
 * MCQ_MULTIPLE → Multiple correct answers from options
 * TRUE_FALSE   → Boolean answer
 * SHORT_TEXT   → Free text answer (manually graded — Phase 2)
 */
public enum QuestionType {
    MCQ_SINGLE,
    MCQ_MULTIPLE,
    TRUE_FALSE,
    SHORT_TEXT
}