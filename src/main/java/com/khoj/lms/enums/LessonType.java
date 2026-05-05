package com.khoj.lms.enums;

/**
 * Type of content a lesson contains.
 *
 * VIDEO       → Primary content type; hosted on AWS S3
 * DOCUMENT    → PDF / notes attached to a lesson
 * QUIZ        → Assessment lesson (links to Quiz entity)
 * ASSIGNMENT  → Practical task / project (links to Assignment entity)
 * TEXT        → Rich text / article (inline content)
 */
public enum LessonType {
    VIDEO,
    DOCUMENT,
    QUIZ,
    ASSIGNMENT,
    TEXT
}