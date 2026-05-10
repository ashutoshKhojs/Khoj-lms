package com.khoj.lms.service.impl;

import com.khoj.lms.dto.lesson.*;
import com.khoj.lms.entity.*;
import com.khoj.lms.entity.Module;
import com.khoj.lms.exception.*;
import com.khoj.lms.repository.*;
import com.khoj.lms.service.LessonService;
import com.khoj.lms.service.ModuleService;
import com.khoj.lms.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonServiceImpl implements LessonService {

    private final LessonRepository      lessonRepository;
    private final ModuleRepository      moduleRepository;
    private final ModuleService         moduleService;
    private final S3Service             s3Service;
    // ================= READ =================

    @Override
    @Transactional(readOnly = true)
    public List<LessonSummary> getLessonsByModule(UUID moduleId) {
        return lessonRepository
                .findByModuleIdAndIsDeletedFalseOrderByDisplayOrderAsc(moduleId)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LessonResponse getLessonById(UUID lessonId) {
        Lesson lesson = findOrThrow(lessonId);
        return toFullResponse(lesson);
    }

    // ================= WRITE =================

    @Override
    @Transactional
    public LessonResponse createLesson(UUID moduleId, LessonRequest request, String instructorEmail) {
        Module module = moduleService.findOrThrow(moduleId);
        assertOwns(module.getCourse(), instructorEmail);

        int order = request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : (lessonRepository.findMaxDisplayOrder(moduleId) != null
                   ? lessonRepository.findMaxDisplayOrder(moduleId) + 1
                   : 1);

        Lesson lesson = Lesson.builder()
                .module(module)
                .course(module.getCourse())
                .title(request.getTitle())
                .description(request.getDescription())
                .lessonType(request.getLessonType())
                .displayOrder(order)
                .videoS3Key(request.getVideoS3Key())
                .videoDurationSeconds(request.getVideoDurationSeconds())
                .videoThumbnailS3Key(request.getVideoThumbnailS3Key())
                .notesS3Key(request.getNotesS3Key())
                .notesFileName(request.getNotesFileName())
                .textContent(request.getTextContent())
                .isPreview(request.getIsPreview() != null ? request.getIsPreview() : false)
                .isPublished(false)
                .build();

        lesson = lessonRepository.save(lesson);

        moduleService.recalculateModuleStats(moduleId);
        moduleService.recalculateCourseStats(module.getCourse().getId());

        log.info("Lesson '{}' created in module {}", lesson.getTitle(), moduleId);

        return toFullResponse(lesson);
    }

    @Override
    @Transactional
    public LessonResponse updateLesson(UUID lessonId, LessonRequest request, String instructorEmail) {
        Lesson lesson = findOrThrow(lessonId);
        assertOwns(lesson.getCourse(), instructorEmail);

        lesson.setTitle(request.getTitle());

        if (request.getDescription() != null) lesson.setDescription(request.getDescription());
        if (request.getDisplayOrder() != null) lesson.setDisplayOrder(request.getDisplayOrder());
        if (request.getVideoS3Key() != null) lesson.setVideoS3Key(request.getVideoS3Key());
        if (request.getVideoDurationSeconds() != null) lesson.setVideoDurationSeconds(request.getVideoDurationSeconds());
        if (request.getVideoThumbnailS3Key() != null) lesson.setVideoThumbnailS3Key(request.getVideoThumbnailS3Key());
        if (request.getNotesS3Key() != null) lesson.setNotesS3Key(request.getNotesS3Key());
        if (request.getNotesFileName() != null) lesson.setNotesFileName(request.getNotesFileName());
        if (request.getTextContent() != null) lesson.setTextContent(request.getTextContent());
        if (request.getIsPreview() != null) lesson.setIsPreview(request.getIsPreview());

        lesson = lessonRepository.save(lesson);

        moduleService.recalculateModuleStats(lesson.getModule().getId());
        moduleService.recalculateCourseStats(lesson.getCourse().getId());

        return toFullResponse(lesson);
    }

    @Override
    @Transactional
    public LessonResponse togglePublish(UUID lessonId, String instructorEmail) {
        Lesson lesson = findOrThrow(lessonId);
        assertOwns(lesson.getCourse(), instructorEmail);

        lesson.setIsPublished(!lesson.getIsPublished());

        return toFullResponse(lessonRepository.save(lesson));
    }

    @Override
    @Transactional
    public void deleteLesson(UUID lessonId, String instructorEmail) {
        Lesson lesson = findOrThrow(lessonId);
        assertOwns(lesson.getCourse(), instructorEmail);

        UUID moduleId = lesson.getModule().getId();
        UUID courseId = lesson.getCourse().getId();

        lesson.softDelete();
        lessonRepository.save(lesson);

        moduleService.recalculateModuleStats(moduleId);
        moduleService.recalculateCourseStats(courseId);

        log.info("Lesson {} soft-deleted", lessonId);
    }

    @Override
    @Transactional
    public void reorderLessons(LessonReorderRequest request, String instructorEmail) {
        Module module = moduleService.findOrThrow(request.getModuleId());
        assertOwns(module.getCourse(), instructorEmail);

        request.getOrder().forEach(item -> {
            Lesson lesson = findOrThrow(item.getLessonId());

            if (!lesson.getModule().getId().equals(request.getModuleId())) {
                throw new BadRequestException(
                        "Lesson " + item.getLessonId() + " does not belong to this module.");
            }

            lesson.setDisplayOrder(item.getDisplayOrder());
            lessonRepository.save(lesson);
        });
    }

    // ================= HELPERS =================

    @Override
    public Lesson findOrThrow(UUID id) {
        return lessonRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
    }


    private void assertOwns(Course course, String email) {
        if (!course.getInstructor().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("You do not own this course.");
        }
    }

    // ================= MAPPERS =================

    private LessonResponse toFullResponse(Lesson l) {

        String videoUrl = l.getVideoS3Key() != null
                ? s3Service.generateStreamUrl(l.getVideoS3Key(), true).getStreamUrl()
                : null;

        String notesUrl = l.getNotesS3Key() != null
                ? s3Service.generateStreamUrl(l.getNotesS3Key(), false).getStreamUrl()
                : null;

        String thumbUrl = l.getVideoThumbnailS3Key() != null
                ? s3Service.generateStreamUrl(l.getVideoThumbnailS3Key(), false).getStreamUrl()
                : null;
        UUID prevId = lessonRepository
                .findPreviousLesson(l.getModule().getId(), l.getDisplayOrder())
                .map(Lesson::getId).orElse(null);

        UUID nextId = lessonRepository
                .findNextLesson(l.getModule().getId(), l.getDisplayOrder())
                .map(Lesson::getId).orElse(null);

        return LessonResponse.builder()
                .id(l.getId())
                .moduleId(l.getModule().getId())
                .courseId(l.getCourse().getId())
                .title(l.getTitle())
                .description(l.getDescription())
                .lessonType(l.getLessonType())
                .displayOrder(l.getDisplayOrder())
                .isPreview(l.getIsPreview())
                .isPublished(l.getIsPublished())
                .videoUrl(videoUrl)
                .videoDurationSeconds(l.getVideoDurationSeconds())
                .videoThumbnailUrl(thumbUrl)
                .notesUrl(notesUrl)
                .notesFileName(l.getNotesFileName())
                .textContent(l.getTextContent())
                .previousLessonId(prevId)
                .nextLessonId(nextId)
                .build();
    }

    private LessonSummary toSummary(Lesson l) {
        return LessonSummary.builder()
                .id(l.getId())
                .title(l.getTitle())
                .lessonType(l.getLessonType())
                .displayOrder(l.getDisplayOrder())
                .videoDurationSeconds(l.getVideoDurationSeconds())
                .isPreview(l.getIsPreview())
                .isCompleted(false)
                .build();
    }
}