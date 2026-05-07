package com.khoj.lms.service;

import com.khoj.lms.dto.lesson.*;
import com.khoj.lms.entity.Lesson;

import java.util.List;
import java.util.UUID;

public interface LessonService {

    // READ
    List<LessonSummary> getLessonsByModule(UUID moduleId);

    LessonResponse getLessonById(UUID lessonId);

    // WRITE
    LessonResponse createLesson(UUID moduleId, LessonRequest request, String instructorEmail);

    LessonResponse updateLesson(UUID lessonId, LessonRequest request, String instructorEmail);

    LessonResponse togglePublish(UUID lessonId, String instructorEmail);

    void deleteLesson(UUID lessonId, String instructorEmail);

    void reorderLessons(LessonReorderRequest request, String instructorEmail);

    // INTERNAL
    Lesson findOrThrow(UUID id);
}