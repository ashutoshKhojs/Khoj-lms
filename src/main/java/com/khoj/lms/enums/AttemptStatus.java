package com.khoj.lms.enums;

/**
 * Status of a student's quiz attempt.
 *
 * IN_PROGRESS → Started but not yet submitted
 * SUBMITTED   → Submitted and auto-evaluated
 * PASSED      → Score >= passing threshold
 * FAILED      → Score < passing threshold (can reattempt if allowed)
 */
public enum AttemptStatus {
    IN_PROGRESS,
    SUBMITTED,
    PASSED,
    FAILED
}