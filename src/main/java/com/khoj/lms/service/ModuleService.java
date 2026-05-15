package com.khoj.lms.service;

import com.khoj.lms.dto.module.ModuleReorderRequest;
import com.khoj.lms.dto.module.ModuleRequest;
import com.khoj.lms.dto.module.ModuleResponse;
import com.khoj.lms.entity.Module;

import java.util.List;
import java.util.UUID;

public interface ModuleService {

    // READ — PUBLIC
    List<ModuleResponse> getModulesByCourse(UUID courseId);
    ModuleResponse getById(UUID moduleId);

    // READ — INSTRUCTOR/ADMIN (sees unpublished too)
    List<ModuleResponse> getModulesByCourseForInstructor(UUID courseId,
                                                         String instructorEmail);

    // WRITE — INSTRUCTOR
    ModuleResponse createModule(UUID courseId, ModuleRequest request,
                                String instructorEmail);
    ModuleResponse updateModule(UUID moduleId, ModuleRequest request,
                                String instructorEmail);
    ModuleResponse togglePublish(UUID moduleId, String instructorEmail);
    void deleteModule(UUID moduleId, String instructorEmail);
    void reorderModules(ModuleReorderRequest request, String instructorEmail);

    // WRITE — ADMIN
    ModuleResponse adminTogglePublish(UUID moduleId);
    void adminDeleteModule(UUID moduleId);

    // INTERNAL
    void recalculateModuleStats(UUID moduleId);
    void recalculateCourseStats(UUID courseId);
    Module findOrThrow(UUID id);

    // ADMIN — view any course modules without ownership check
    List<ModuleResponse> adminGetModulesByCourse(UUID courseId);
}