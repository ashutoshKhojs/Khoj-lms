package com.khoj.lms.service.impl;

import com.khoj.lms.audit.AuditLogger;
import com.khoj.lms.dto.lesson.LessonSummary;
import com.khoj.lms.dto.module.*;
import com.khoj.lms.entity.*;
import com.khoj.lms.entity.Module;
import com.khoj.lms.enums.CourseStatus;
import com.khoj.lms.exception.*;
import com.khoj.lms.repository.*;
import com.khoj.lms.service.ModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModuleServiceImpl implements ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final AuditLogger      auditLogger;

    // ================= READ — PUBLIC =================

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponse> getModulesByCourse(UUID courseId) {
        log.debug("Fetching published modules for courseId={}", courseId);

        // Validate course exists
        Course course = findCourseOrThrow(courseId);

        // Public can only see modules of PUBLISHED courses
        if (!course.isPublished()) {
            throw new ResourceNotFoundException("Course", "id", courseId);
        }

        List<ModuleResponse> modules = moduleRepository
                .findByCourseIdAndIsDeletedFalseOrderByDisplayOrderAsc(courseId)
                .stream()
                // Public only sees published modules
                .filter(m -> m.getIsPublished())
                .map(this::toResponse)
                .collect(Collectors.toList());

        log.debug("Found {} published modules for courseId={}",
                modules.size(), courseId);
        return modules;
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleResponse getById(UUID moduleId) {
        log.debug("Fetching module id={}", moduleId);
        return toResponse(findOrThrow(moduleId));
    }

    // ================= READ — INSTRUCTOR =================

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponse> getModulesByCourseForInstructor(
            UUID courseId, String instructorEmail) {

        log.debug("Fetching all modules for courseId={} instructor={}",
                courseId, instructorEmail);

        Course course = findCourseOrThrow(courseId);
        assertOwns(course, instructorEmail);

        // Instructor sees ALL modules (published + unpublished)
        List<ModuleResponse> modules = moduleRepository
                .findByCourseIdAndIsDeletedFalseOrderByDisplayOrderAsc(courseId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        log.debug("Found {} modules for courseId={} instructor={}",
                modules.size(), courseId, instructorEmail);
        return modules;
    }

    // ================= WRITE — INSTRUCTOR =================

    @Override
    @Transactional
    public ModuleResponse createModule(UUID courseId,
                                       ModuleRequest request,
                                       String instructorEmail) {

        log.info("Creating module courseId={} title='{}' instructor={}",
                courseId, request.getTitle(), instructorEmail);

        Course course = findCourseOrThrow(courseId);
        assertOwns(course, instructorEmail);

        // Can only add modules to DRAFT or REJECTED courses
        if (course.getStatus() == CourseStatus.PUBLISHED ||
                course.getStatus() == CourseStatus.ARCHIVED) {
            throw new BadRequestException(
                    "Cannot add modules to a " + course.getStatus().name() +
                            " course. Archive or reject it first.");
        }

        int order = request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : (moduleRepository.findMaxDisplayOrder(courseId) != null
                   ? moduleRepository.findMaxDisplayOrder(courseId) + 1
                   : 1);

        log.debug("Assigned displayOrder={} for new module in courseId={}",
                order, courseId);

        Module module = Module.builder()
                .course(course)
                .title(request.getTitle())
                .description(request.getDescription())
                .displayOrder(order)
                .isLocked(request.getIsLocked() != null
                        ? request.getIsLocked() : false)
                .isPublished(false)
                .build();

        module = moduleRepository.save(module);
        recalculateCourseStats(courseId);

        log.info("Module created: id={} title='{}' courseId={}",
                module.getId(), module.getTitle(), courseId);

        auditLogger.adminAction(instructorEmail,
                "MODULE_CREATED: " + module.getTitle(), null);

        return toResponse(module);
    }

    @Override
    @Transactional
    public ModuleResponse updateModule(UUID moduleId,
                                       ModuleRequest request,
                                       String instructorEmail) {

        log.info("Updating module id={} instructor={}", moduleId, instructorEmail);

        Module module = findOrThrow(moduleId);
        assertOwns(module.getCourse(), instructorEmail);

        // Cannot edit modules of PUBLISHED courses
        CourseStatus courseStatus = module.getCourse().getStatus();
        if (courseStatus == CourseStatus.PUBLISHED ||
                courseStatus == CourseStatus.ARCHIVED) {
            throw new BadRequestException(
                    "Cannot edit modules of a " + courseStatus.name() + " course.");
        }

        String oldTitle = module.getTitle();
        module.setTitle(request.getTitle());

        if (request.getDescription()  != null)
            module.setDescription(request.getDescription());
        if (request.getDisplayOrder() != null)
            module.setDisplayOrder(request.getDisplayOrder());
        if (request.getIsLocked()     != null)
            module.setIsLocked(request.getIsLocked());

        Module saved = moduleRepository.save(module);

        log.info("Module updated: id={} oldTitle='{}' newTitle='{}'",
                moduleId, oldTitle, saved.getTitle());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ModuleResponse togglePublish(UUID moduleId, String instructorEmail) {

        log.info("Toggling publish for module id={} instructor={}",
                moduleId, instructorEmail);

        Module module = findOrThrow(moduleId);
        assertOwns(module.getCourse(), instructorEmail);

        boolean wasPublished = module.getIsPublished();
        module.setIsPublished(!wasPublished);
        Module saved = moduleRepository.save(module);

        log.info("Module id={} title='{}' publish toggled: {} → {}",
                moduleId, saved.getTitle(), wasPublished, saved.getIsPublished());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteModule(UUID moduleId, String instructorEmail) {

        log.info("Deleting module id={} instructor={}", moduleId, instructorEmail);

        Module module = findOrThrow(moduleId);
        assertOwns(module.getCourse(), instructorEmail);

        // Cannot delete module from PUBLISHED course
        if (module.getCourse().getStatus() == CourseStatus.PUBLISHED) {
            throw new BadRequestException(
                    "Cannot delete modules from a PUBLISHED course. " +
                            "Archive the course first.");
        }

        String title    = module.getTitle();
        UUID   courseId = module.getCourse().getId();

        module.softDelete();
        moduleRepository.save(module);
        recalculateCourseStats(courseId);

        log.info("Module soft-deleted: id={} title='{}' courseId={}",
                moduleId, title, courseId);

        auditLogger.adminAction(instructorEmail,
                "MODULE_DELETED: " + title, null);
    }

    @Override
    @Transactional
    public void reorderModules(ModuleReorderRequest request,
                               String instructorEmail) {

        log.info("Reordering modules in courseId={} instructor={}",
                request.getCourseId(), instructorEmail);

        Course course = findCourseOrThrow(request.getCourseId());
        assertOwns(course, instructorEmail);

        request.getOrder().forEach(item -> {
            Module module = findOrThrow(item.getModuleId());

            if (!module.getCourse().getId().equals(request.getCourseId())) {
                log.warn("Reorder rejected — module id={} courseId={}",
                        item.getModuleId(), request.getCourseId());
                throw new BadRequestException(
                        "Module " + item.getModuleId() +
                                " does not belong to this course.");
            }

            module.setDisplayOrder(item.getDisplayOrder());
            moduleRepository.save(module);
        });

        log.info("Modules reordered in courseId={} count={}",
                request.getCourseId(), request.getOrder().size());
    }

    // ================= WRITE — ADMIN =================

    @Override
    @Transactional
    public ModuleResponse adminTogglePublish(UUID moduleId) {

        log.info("Admin toggling publish for module id={}", moduleId);

        Module module = findOrThrow(moduleId);

        boolean wasPublished = module.getIsPublished();
        module.setIsPublished(!wasPublished);
        Module saved = moduleRepository.save(module);

        log.info("Admin toggled module id={} title='{}' publish: {} → {}",
                moduleId, saved.getTitle(), wasPublished, saved.getIsPublished());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void adminDeleteModule(UUID moduleId) {

        log.info("Admin deleting module id={}", moduleId);

        Module module = findOrThrow(moduleId);

        String title    = module.getTitle();
        UUID   courseId = module.getCourse().getId();

        module.softDelete();
        moduleRepository.save(module);
        recalculateCourseStats(courseId);

        log.info("Admin soft-deleted module: id={} title='{}' courseId={}",
                moduleId, title, courseId);

        auditLogger.adminAction(
                "admin",
                "ADMIN_MODULE_DELETED: " + title,
                null
        );
    }

    // ================= STATS =================

    @Override
    public void recalculateModuleStats(UUID moduleId) {
        log.debug("Recalculating stats for moduleId={}", moduleId);

        int  lessons  = lessonRepository.countByModuleId(moduleId);
        long duration = lessonRepository.sumDurationByModuleId(moduleId);

        moduleRepository.updateStats(moduleId, lessons, duration);

        log.debug("Module stats updated: moduleId={} lessons={} duration={}s",
                moduleId, lessons, duration);
    }

    @Override
    public void recalculateCourseStats(UUID courseId) {
        log.debug("Recalculating stats for courseId={}", courseId);

        List<Module> modules = moduleRepository
                .findByCourseIdAndIsDeletedFalseOrderByDisplayOrderAsc(courseId);

        int  totalModules  = modules.size();
        int  totalLessons  = lessonRepository.countByCourseId(courseId);
        long totalDuration = lessonRepository.sumDurationByCourseId(courseId);

        courseRepository.updateStats(courseId, totalModules,
                totalLessons, totalDuration);

        log.debug("Course stats updated: courseId={} modules={} lessons={} duration={}s",
                courseId, totalModules, totalLessons, totalDuration);
    }

    // ================= HELPERS =================

    @Override
    public Module findOrThrow(UUID id) {
        return moduleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Module", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponse> adminGetModulesByCourse(UUID courseId) {
        log.debug("Admin fetching all modules for courseId={}", courseId);

        // Validate course exists
        findCourseOrThrow(courseId);

        // Admin sees ALL modules — no published filter, no ownership check
        List<ModuleResponse> modules = moduleRepository
                .findByCourseIdAndIsDeletedFalseOrderByDisplayOrderAsc(courseId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        log.debug("Admin found {} modules for courseId={}", modules.size(), courseId);
        return modules;
    }

    private Course findCourseOrThrow(UUID id) {
        return courseRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "id", id));
    }

    private void assertOwns(Course course, String email) {
        if (!course.getInstructor().getEmail().equalsIgnoreCase(email)) {
            log.warn("Ownership check failed — email={} courseId={}",
                    email, course.getId());
            throw new AccessDeniedException("You do not own this course.");
        }
    }

    // ================= MAPPER =================

    private ModuleResponse toResponse(Module m) {
        List<LessonSummary> lessonSummaries = m.getLessons().stream()
                .filter(l -> !l.getIsDeleted())
                .sorted(Comparator.comparingInt(Lesson::getDisplayOrder))
                .map(l -> LessonSummary.builder()
                        .id(l.getId())
                        .title(l.getTitle())
                        .lessonType(l.getLessonType())
                        .displayOrder(l.getDisplayOrder())
                        .videoDurationSeconds(l.getVideoDurationSeconds())
                        .isPreview(l.getIsPreview())
                        .isCompleted(false)
                        .build())
                .collect(Collectors.toList());

        return ModuleResponse.builder()
                .id(m.getId())
                .courseId(m.getCourse().getId())
                .title(m.getTitle())
                .description(m.getDescription())
                .displayOrder(m.getDisplayOrder())
                .isPublished(m.getIsPublished())
                .isLocked(m.getIsLocked())
                .totalLessons(m.getTotalLessons())
                .totalDurationSeconds(m.getTotalDurationSeconds())
                .lessons(lessonSummaries)
                .build();
    }
}