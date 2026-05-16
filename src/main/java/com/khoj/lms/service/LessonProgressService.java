package com.khoj.lms.service;

import com.khoj.lms.dto.progress.*;

import java.util.UUID;

public interface LessonProgressService {

    /** Called when student opens a lesson page. Idempotent — creates row on first access. */
    LessonProgressResponse recordAccess(UUID lessonId, String studentEmail);

    /** Heartbeat from video player — updates position, auto-completes at 90%. */
    LessonProgressResponse updateWatchPosition(UUID lessonId,
                                               WatchPositionRequest request,
                                               String studentEmail);

    /** Manual "Mark Complete" button (for non-video lessons or override). */
    LessonProgressResponse markCompleted(UUID lessonId, String studentEmail);

    /** Get one lesson's progress for current student. */
    LessonProgressResponse getMyLessonProgress(UUID lessonId, String studentEmail);

    /** Get all my lesson progress for one course. */
    CourseProgressSummary getMyCourseProgress(UUID courseId, String studentEmail);

    /** Admin view — any student's progress in any course. */
    CourseProgressSummary adminGetCourseProgress(UUID courseId, UUID studentId);
}