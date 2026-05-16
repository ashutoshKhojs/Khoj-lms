package com.khoj.lms.service.impl;

import com.khoj.lms.audit.AuditLogger;
import com.khoj.lms.dto.lesson.*;
import com.khoj.lms.entity.*;
import com.khoj.lms.entity.Module;
import com.khoj.lms.enums.CourseStatus;
import com.khoj.lms.enums.LessonType;
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

    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final ModuleService    moduleService;
    private final S3Service        s3Service;
    private final AuditLogger      auditLogger;

    // ═══════════════════════════════════════════════════════════
    // READ — PUBLIC (published lessons of published courses only)
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<LessonSummary> getLessonsByModule(UUID moduleId) {
        log.debug("[PUBLIC] Fetching published lessons for moduleId={}", moduleId);

        Module module = moduleService.findOrThrow(moduleId);
        assertCoursePublished(module.getCourse());
        assertModulePublished(module);

        List<LessonSummary> lessons = lessonRepository
                .findPublishedByModuleId(moduleId)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        log.debug("[PUBLIC] Found {} published lessons in moduleId={}", lessons.size(), moduleId);
        return lessons;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonSummary> getLessonsByCourse(UUID courseId) {
        log.debug("[PUBLIC] Fetching published lessons for courseId={}", courseId);

        // course existence + status validated by repository query results
        List<LessonSummary> lessons = lessonRepository
                .findPublishedByCourseId(courseId)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        log.debug("[PUBLIC] Found {} published lessons in courseId={}", lessons.size(), courseId);
        return lessons;
    }

    @Override
    @Transactional(readOnly = true)
    public LessonResponse getLessonById(UUID lessonId) {
        log.debug("[PUBLIC] Fetching lesson id={}", lessonId);

        Lesson lesson = findOrThrow(lessonId);

        // Public sees only published lessons of published courses
        // — unless it's a preview lesson (then course must still be published)
        assertCoursePublished(lesson.getCourse());

        if (!Boolean.TRUE.equals(lesson.getIsPublished()) && !Boolean.TRUE.equals(lesson.getIsPreview())) {
            log.warn("[PUBLIC] Attempt to view unpublished non-preview lesson id={}", lessonId);
            throw new ResourceNotFoundException("Lesson", "id", lessonId);
        }

        return toFullResponse(lesson);
    }

    // ═══════════════════════════════════════════════════════════
    // READ — INSTRUCTOR (sees own drafts)
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<LessonSummary> getLessonsByModuleForInstructor(UUID moduleId, String instructorEmail) {
        log.debug("[INSTRUCTOR] Fetching all lessons for moduleId={} by {}", moduleId, instructorEmail);

        Module module = moduleService.findOrThrow(moduleId);
        assertOwns(module.getCourse(), instructorEmail);

        return lessonRepository
                .findByModuleIdAndIsDeletedFalseOrderByDisplayOrderAsc(moduleId)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonSummary> getLessonsByCourseForInstructor(UUID courseId, String instructorEmail) {
        log.debug("[INSTRUCTOR] Fetching all lessons for courseId={} by {}", courseId, instructorEmail);

        // Get one lesson to find the course, then check ownership
        List<Lesson> lessons = lessonRepository
                .findByCourseIdAndIsDeletedFalseOrderByDisplayOrderAsc(courseId);

        if (!lessons.isEmpty()) {
            assertOwns(lessons.get(0).getCourse(), instructorEmail);
        }
        // If there are no lessons yet, we silently allow (instructor with valid course will have one)

        return lessons.stream().map(this::toSummary).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LessonResponse getLessonByIdForInstructor(UUID lessonId, String instructorEmail) {
        log.debug("[INSTRUCTOR] Fetching lesson id={} by {}", lessonId, instructorEmail);

        Lesson lesson = findOrThrow(lessonId);
        assertOwns(lesson.getCourse(), instructorEmail);

        return toFullResponse(lesson);
    }

    // ═══════════════════════════════════════════════════════════
    // READ — ADMIN (sees everything)
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<LessonSummary> adminGetLessonsByModule(UUID moduleId) {
        log.debug("[ADMIN] Fetching all lessons for moduleId={}", moduleId);

        return lessonRepository
                .findByModuleIdAndIsDeletedFalseOrderByDisplayOrderAsc(moduleId)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonSummary> adminGetLessonsByCourse(UUID courseId) {
        log.debug("[ADMIN] Fetching all lessons for courseId={}", courseId);

        return lessonRepository
                .findByCourseIdAndIsDeletedFalseOrderByDisplayOrderAsc(courseId)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LessonResponse adminGetLessonById(UUID lessonId) {
        log.debug("[ADMIN] Fetching lesson id={}", lessonId);
        return toFullResponse(findOrThrow(lessonId));
    }

    // ═══════════════════════════════════════════════════════════
    // WRITE — INSTRUCTOR
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public LessonResponse createLesson(UUID moduleId,
                                       LessonRequest request,
                                       String instructorEmail) {

        log.info("Creating lesson moduleId={} title='{}' type={} instructor={}",
                moduleId, request.getTitle(), request.getLessonType(), instructorEmail);

        Module module = moduleService.findOrThrow(moduleId);
        Course course = module.getCourse();

        assertOwns(course, instructorEmail);
        assertCourseEditable(course);
        validateLessonContent(request);

        // Compute display order ONCE (not twice as before)
        int order;
        if (request.getDisplayOrder() != null) {
            order = request.getDisplayOrder();
        } else {
            Integer max = lessonRepository.findMaxDisplayOrder(moduleId);
            order = (max != null) ? max + 1 : 1;
        }

        log.debug("Assigned displayOrder={} for new lesson in moduleId={}", order, moduleId);

        Lesson lesson = Lesson.builder()
                .module(module)
                .course(course)
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
        moduleService.recalculateCourseStats(course.getId());

        log.info("Lesson created: id={} title='{}' type={} moduleId={}",
                lesson.getId(), lesson.getTitle(), lesson.getLessonType(), moduleId);

        auditLogger.adminAction(
                instructorEmail,
                "LESSON_CREATED: " + lesson.getTitle(),
                null
        );

        return toFullResponse(lesson);
    }

    @Override
    @Transactional
    public LessonResponse updateLesson(UUID lessonId,
                                       LessonRequest request,
                                       String instructorEmail) {

        log.info("Updating lesson id={} instructor={}", lessonId, instructorEmail);

        Lesson lesson = findOrThrow(lessonId);
        assertOwns(lesson.getCourse(), instructorEmail);
        assertCourseEditable(lesson.getCourse());

        String oldTitle = lesson.getTitle();

        // Null-safe updates — only overwrite fields the caller provided
        if (request.getTitle()               != null) lesson.setTitle(request.getTitle());
        if (request.getDescription()         != null) lesson.setDescription(request.getDescription());
        if (request.getLessonType()          != null) lesson.setLessonType(request.getLessonType());
        if (request.getDisplayOrder()        != null) lesson.setDisplayOrder(request.getDisplayOrder());
        if (request.getVideoS3Key()          != null) lesson.setVideoS3Key(request.getVideoS3Key());
        if (request.getVideoDurationSeconds()!= null) lesson.setVideoDurationSeconds(request.getVideoDurationSeconds());
        if (request.getVideoThumbnailS3Key() != null) lesson.setVideoThumbnailS3Key(request.getVideoThumbnailS3Key());
        if (request.getNotesS3Key()          != null) lesson.setNotesS3Key(request.getNotesS3Key());
        if (request.getNotesFileName()       != null) lesson.setNotesFileName(request.getNotesFileName());
        if (request.getTextContent()         != null) lesson.setTextContent(request.getTextContent());
        if (request.getIsPreview()           != null) lesson.setIsPreview(request.getIsPreview());

        // Re-validate content after merge so partial updates can't leave an invalid lesson
        validateLessonContentForEntity(lesson);

        lesson = lessonRepository.save(lesson);

        moduleService.recalculateModuleStats(lesson.getModule().getId());
        moduleService.recalculateCourseStats(lesson.getCourse().getId());

        log.info("Lesson updated: id={} oldTitle='{}' newTitle='{}'",
                lessonId, oldTitle, lesson.getTitle());

        return toFullResponse(lesson);
    }

    @Override
    @Transactional
    public LessonResponse togglePublish(UUID lessonId, String instructorEmail) {
        log.info("Toggling publish for lesson id={} instructor={}", lessonId, instructorEmail);

        Lesson lesson = findOrThrow(lessonId);
        assertOwns(lesson.getCourse(), instructorEmail);
        assertCourseEditable(lesson.getCourse());

        boolean wasPublished = Boolean.TRUE.equals(lesson.getIsPublished());

        // If turning ON, ensure the lesson has the content its type requires
        if (!wasPublished) {
            validateLessonContentForEntity(lesson);
        }

        lesson.setIsPublished(!wasPublished);
        lesson = lessonRepository.save(lesson);

        log.info("Lesson id={} title='{}' publish toggled: {} → {}",
                lessonId, lesson.getTitle(), wasPublished, lesson.getIsPublished());

        return toFullResponse(lesson);
    }

    @Override
    @Transactional
    public void deleteLesson(UUID lessonId, String instructorEmail) {
        log.info("Deleting lesson id={} instructor={}", lessonId, instructorEmail);

        Lesson lesson = findOrThrow(lessonId);
        assertOwns(lesson.getCourse(), instructorEmail);
        assertCourseEditable(lesson.getCourse());

        UUID moduleId = lesson.getModule().getId();
        UUID courseId = lesson.getCourse().getId();
        String title  = lesson.getTitle();

        lesson.softDelete();
        lessonRepository.save(lesson);

        moduleService.recalculateModuleStats(moduleId);
        moduleService.recalculateCourseStats(courseId);

        log.info("Lesson soft-deleted: id={} title='{}' moduleId={}", lessonId, title, moduleId);

        auditLogger.adminAction(
                instructorEmail,
                "LESSON_DELETED: " + title,
                null
        );
    }

    @Override
    @Transactional
    public void reorderLessons(LessonReorderRequest request, String instructorEmail) {
        log.info("Reordering lessons in moduleId={} instructor={} count={}",
                request.getModuleId(), instructorEmail, request.getOrder().size());

        Module module = moduleService.findOrThrow(request.getModuleId());
        assertOwns(module.getCourse(), instructorEmail);
        assertCourseEditable(module.getCourse());

        request.getOrder().forEach(item -> {
            Lesson lesson = findOrThrow(item.getLessonId());

            if (!lesson.getModule().getId().equals(request.getModuleId())) {
                log.warn("Reorder rejected — lesson id={} does not belong to moduleId={}",
                        item.getLessonId(), request.getModuleId());

                throw new BadRequestException(
                        "Lesson " + item.getLessonId() + " does not belong to this module.");
            }

            lesson.setDisplayOrder(item.getDisplayOrder());
            lessonRepository.save(lesson);
        });

        log.info("Lessons reordered in moduleId={} count={}",
                request.getModuleId(), request.getOrder().size());
    }

    // ═══════════════════════════════════════════════════════════
    // WRITE — ADMIN
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public LessonResponse adminTogglePublish(UUID lessonId) {
        log.info("[ADMIN] Toggling publish for lesson id={}", lessonId);

        Lesson lesson = findOrThrow(lessonId);

        boolean wasPublished = Boolean.TRUE.equals(lesson.getIsPublished());
        if (!wasPublished) {
            validateLessonContentForEntity(lesson);
        }
        lesson.setIsPublished(!wasPublished);
        lesson = lessonRepository.save(lesson);

        log.info("[ADMIN] Lesson id={} publish toggled: {} → {}",
                lessonId, wasPublished, lesson.getIsPublished());

        auditLogger.adminAction(
                "admin",
                "ADMIN_LESSON_TOGGLE_PUBLISH: " + lesson.getTitle(),
                null
        );

        return toFullResponse(lesson);
    }

    @Override
    @Transactional
    public void adminDeleteLesson(UUID lessonId) {
        log.info("[ADMIN] Deleting lesson id={}", lessonId);

        Lesson lesson = findOrThrow(lessonId);

        UUID moduleId = lesson.getModule().getId();
        UUID courseId = lesson.getCourse().getId();
        String title  = lesson.getTitle();

        lesson.softDelete();
        lessonRepository.save(lesson);

        moduleService.recalculateModuleStats(moduleId);
        moduleService.recalculateCourseStats(courseId);

        log.info("[ADMIN] Lesson soft-deleted: id={} title='{}' moduleId={}",
                lessonId, title, moduleId);

        auditLogger.adminAction(
                "admin",
                "ADMIN_LESSON_DELETED: " + title,
                null
        );
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    @Override
    public Lesson findOrThrow(UUID id) {
        return lessonRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
    }

    private void assertOwns(Course course, String email) {
        if (!course.getInstructor().getEmail().equalsIgnoreCase(email)) {
            log.warn("Ownership check failed — email={} does not own courseId={}",
                    email, course.getId());
            throw new AccessDeniedException("You do not own this course.");
        }
    }

    /** Instructor can only edit DRAFT or REJECTED courses. */
    private void assertCourseEditable(Course course) {
        CourseStatus s = course.getStatus();
        if (s == CourseStatus.PUBLISHED) {
            throw new BadRequestException(
                    "Cannot modify lessons of a PUBLISHED course. Submit a new version instead.");
        }
        if (s == CourseStatus.ARCHIVED) {
            throw new BadRequestException("Cannot modify lessons of an ARCHIVED course.");
        }
        if (s == CourseStatus.PENDING) {
            throw new BadRequestException(
                    "Course is under review. Wait for approval or rejection before editing lessons.");
        }
    }

    private void assertCoursePublished(Course course) {
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            log.warn("[PUBLIC] Attempt to access non-published course: id={} status={}",
                    course.getId(), course.getStatus());
            throw new ResourceNotFoundException("Course", "id", course.getId());
        }
    }

    private void assertModulePublished(Module module) {
        if (!Boolean.TRUE.equals(module.getIsPublished())) {
            log.warn("[PUBLIC] Attempt to access unpublished module: id={}", module.getId());
            throw new ResourceNotFoundException("Module", "id", module.getId());
        }
    }

    /** Validates request payload before building entity (create path). */
    private void validateLessonContent(LessonRequest req) {
        if (req.getLessonType() == null) {
            throw new BadRequestException("Lesson type is required.");
        }
        switch (req.getLessonType()) {
            case VIDEO -> {
                if (isBlank(req.getVideoS3Key())) {
                    throw new BadRequestException(
                            "VIDEO lesson requires videoS3Key. Upload the video first.");
                }
            }
            case DOCUMENT -> {
                if (isBlank(req.getNotesS3Key())) {
                    throw new BadRequestException(
                            "DOCUMENT lesson requires notesS3Key. Upload the document first.");
                }
            }
            case TEXT -> {
                if (isBlank(req.getTextContent())) {
                    throw new BadRequestException("TEXT lesson requires textContent.");
                }
            }
            case QUIZ, ASSIGNMENT -> {
                // Quiz and Assignment entities are attached later by their own services.
                // No content required at lesson-creation time.
            }
        }
    }

    /** Validates entity state (used on update + publish, after merge). */
    private void validateLessonContentForEntity(Lesson l) {
        if (l.getLessonType() == null) {
            throw new BadRequestException("Lesson type is required.");
        }
        switch (l.getLessonType()) {
            case VIDEO -> {
                if (isBlank(l.getVideoS3Key())) {
                    throw new BadRequestException(
                            "VIDEO lesson must have a video uploaded before publish.");
                }
            }
            case DOCUMENT -> {
                if (isBlank(l.getNotesS3Key())) {
                    throw new BadRequestException(
                            "DOCUMENT lesson must have a document uploaded before publish.");
                }
            }
            case TEXT -> {
                if (isBlank(l.getTextContent())) {
                    throw new BadRequestException("TEXT lesson must have content before publish.");
                }
            }
            case QUIZ -> {
                if (l.getQuiz() == null) {
                    throw new BadRequestException("QUIZ lesson must have a quiz attached before publish.");
                }
            }
            case ASSIGNMENT -> {
                if (l.getAssignment() == null) {
                    throw new BadRequestException("ASSIGNMENT lesson must have an assignment attached before publish.");
                }
            }
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ═══════════════════════════════════════════════════════════
    // MAPPERS
    // ═══════════════════════════════════════════════════════════

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
                .quizId(l.getQuiz() != null ? l.getQuiz().getId() : null)
                .assignmentId(l.getAssignment() != null ? l.getAssignment().getId() : null)
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
                .isCompleted(false) // TODO Wk 7 — wire to LessonProgress
                .build();
    }
}