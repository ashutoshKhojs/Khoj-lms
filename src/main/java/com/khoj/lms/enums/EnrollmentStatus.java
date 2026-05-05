package com.khoj.lms.enums;

/**
 * Status of a student's enrollment in a course.
 *
 * ACTIVE      → Currently enrolled and learning
 * COMPLETED   → All lessons done, certificate eligible
 * DROPPED     → Student voluntarily left the course
 * EXPIRED     → Enrollment period ended (for paid/timed courses — Phase 2)
 */
public enum EnrollmentStatus {
    ACTIVE,
    COMPLETED,
    DROPPED,
    EXPIRED
}