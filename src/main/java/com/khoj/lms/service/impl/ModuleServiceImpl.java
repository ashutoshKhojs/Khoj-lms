package com.khoj.lms.service.impl;

import com.khoj.lms.dto.module.*;
import com.khoj.lms.entity.*;
import com.khoj.lms.entity.Module;
import com.khoj.lms.exception.*;
import com.khoj.lms.repository.*;
import com.khoj.lms.service.CourseService;
import com.khoj.lms.service.ModuleService;
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
public class ModuleServiceImpl implements ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final CourseService courseService;

    // ================= READ =================

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponse> getModulesByCourse(UUID courseId) {
        return moduleRepository
                .findByCourseIdAndIsDeletedFalseOrderByDisplayOrderAsc(courseId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleResponse getById(UUID moduleId) {
        return toResponse(findOrThrow(moduleId));
    }

    // ================= WRITE =================

    @Override
    @Transactional
    public ModuleResponse createModule(UUID courseId, ModuleRequest request, String instructorEmail) {
        Course course = courseService.findCourseOrThrow(courseId);
        assertOwns(course, instructorEmail);

        int order = request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : (moduleRepository.findMaxDisplayOrder(courseId) != null
                   ? moduleRepository.findMaxDisplayOrder(courseId) + 1
                   : 1);

        Module module = Module.builder()
                .course(course)
                .title(request.getTitle())
                .description(request.getDescription())
                .displayOrder(order)
                .isLocked(request.getIsLocked() != null ? request.getIsLocked() : false)
                .isPublished(false)
                .build();

        module = moduleRepository.save(module);

        recalculateCourseStats(courseId);

        log.info("Module '{}' created in course {}", module.getTitle(), courseId);

        return toResponse(module);
    }

    @Override
    @Transactional
    public ModuleResponse updateModule(UUID moduleId, ModuleRequest request, String instructorEmail) {
        Module module = findOrThrow(moduleId);
        assertOwns(module.getCourse(), instructorEmail);

        module.setTitle(request.getTitle());

        if (request.getDescription() != null) module.setDescription(request.getDescription());
        if (request.getDisplayOrder() != null) module.setDisplayOrder(request.getDisplayOrder());
        if (request.getIsLocked() != null) module.setIsLocked(request.getIsLocked());

        return toResponse(moduleRepository.save(module));
    }

    @Override
    @Transactional
    public ModuleResponse togglePublish(UUID moduleId, String instructorEmail) {
        Module module = findOrThrow(moduleId);
        assertOwns(module.getCourse(), instructorEmail);

        module.setIsPublished(!module.getIsPublished());

        return toResponse(moduleRepository.save(module));
    }

    @Override
    @Transactional
    public void deleteModule(UUID moduleId, String instructorEmail) {
        Module module = findOrThrow(moduleId);
        assertOwns(module.getCourse(), instructorEmail);

        module.softDelete();
        moduleRepository.save(module);

        recalculateCourseStats(module.getCourse().getId());

        log.info("Module {} soft-deleted", moduleId);
    }

    @Override
    @Transactional
    public void reorderModules(ModuleReorderRequest request, String instructorEmail) {
        Course course = courseService.findCourseOrThrow(request.getCourseId());
        assertOwns(course, instructorEmail);

        request.getOrder().forEach(item -> {
            Module module = findOrThrow(item.getModuleId());

            if (!module.getCourse().getId().equals(request.getCourseId())) {
                throw new BadRequestException(
                        "Module " + item.getModuleId() + " does not belong to this course.");
            }

            module.setDisplayOrder(item.getDisplayOrder());
            moduleRepository.save(module);
        });
    }

    // ================= STATS =================

    @Override
    public void recalculateModuleStats(UUID moduleId) {
        int lessons = lessonRepository.countByModuleId(moduleId);
        long duration = lessonRepository.sumDurationByModuleId(moduleId);

        moduleRepository.updateStats(moduleId, lessons, duration);
    }

    @Override
    public void recalculateCourseStats(UUID courseId) {
        List<Module> modules = moduleRepository
                .findByCourseIdAndIsDeletedFalseOrderByDisplayOrderAsc(courseId);

        int totalModules = modules.size();
        int totalLessons = lessonRepository.countByCourseId(courseId);
        long totalDuration = lessonRepository.sumDurationByCourseId(courseId);

        courseRepository.updateStats(courseId, totalModules, totalLessons, totalDuration);
    }

    // ================= HELPERS =================

    @Override
    public Module findOrThrow(UUID id) {
        return moduleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", id));
    }

    private void assertOwns(Course course, String email) {
        if (!course.getInstructor().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("You do not own this course.");
        }
    }

    private ModuleResponse toResponse(Module m) {
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
                .build();
    }
}