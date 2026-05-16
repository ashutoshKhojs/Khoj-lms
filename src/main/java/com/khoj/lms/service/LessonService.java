package com.khoj.lms.service;

import com.khoj.lms.dto.lesson.*;
import com.khoj.lms.entity.Lesson;

import java.util.List;
import java.util.UUID;

public interface LessonService {

    // ───── READ — PUBLIC (published lessons of published courses only) ─────
    List<LessonSummary> getLessonsByModule(UUID moduleId);

    List<LessonSummary> getLessonsByCourse(UUID courseId);

    LessonResponse getLessonById(UUID lessonId);

    // ───── READ — INSTRUCTOR (sees own drafts too) ─────
    List<LessonSummary> getLessonsByModuleForInstructor(UUID moduleId, String instructorEmail);

    List<LessonSummary> getLessonsByCourseForInstructor(UUID courseId, String instructorEmail);

    LessonResponse getLessonByIdForInstructor(UUID lessonId, String instructorEmail);

    // ───── READ — ADMIN (sees everything) ─────
    List<LessonSummary> adminGetLessonsByModule(UUID moduleId);

    List<LessonSummary> adminGetLessonsByCourse(UUID courseId);

    LessonResponse adminGetLessonById(UUID lessonId);

    // ───── WRITE — INSTRUCTOR ─────
    LessonResponse createLesson(UUID moduleId, LessonRequest request, String instructorEmail);

    LessonResponse updateLesson(UUID lessonId, LessonRequest request, String instructorEmail);

    LessonResponse togglePublish(UUID lessonId, String instructorEmail);

    void deleteLesson(UUID lessonId, String instructorEmail);

    void reorderLessons(LessonReorderRequest request, String instructorEmail);

    // ───── WRITE — ADMIN ─────
    LessonResponse adminTogglePublish(UUID lessonId);

    void adminDeleteLesson(UUID lessonId);

    // ───── INTERNAL ─────
    Lesson findOrThrow(UUID id);
}