package com.khoj.lms.enums;

/**
 * Lifecycle of a course on the platform.
 *
 * DRAFT       → Instructor is still building it (not visible to students)
 * PENDING     → Submitted for admin review/approval
 * PUBLISHED   → Live and visible to students
 * ARCHIVED    → Hidden from listings but accessible to enrolled students
 * REJECTED    → Admin rejected — instructor can edit and resubmit
 */
public enum CourseStatus {
    DRAFT,
    PENDING,
    PUBLISHED,
    ARCHIVED,
    REJECTED
}