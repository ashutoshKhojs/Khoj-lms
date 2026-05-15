package com.khoj.lms.controller;

import com.khoj.lms.dto.common.ApiResponse;
import com.khoj.lms.dto.module.*;
import com.khoj.lms.service.ModuleService;
import com.khoj.lms.util.ApiRoutes;
import com.khoj.lms.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Modules", description = "Course module management — public viewing, instructor authoring, admin control")
public class ModuleController {

    private final ModuleService moduleService;

    // ─────────────────────────────────────────
    // PUBLIC ENDPOINTS
    // ─────────────────────────────────────────

    @GetMapping(ApiRoutes.Module.BASE + ApiRoutes.Module.GET_BY_COURSE)
    @Operation(summary = "List published modules of a published course (public)")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> getModulesByCourse(
            @PathVariable UUID courseId) {

        return ResponseEntity.ok(ApiResponse.success(
                "Modules fetched successfully",
                moduleService.getModulesByCourse(courseId)
        ));
    }

    @GetMapping(ApiRoutes.Module.BASE + ApiRoutes.Module.GET_BY_ID)
    @Operation(summary = "Get a single module by ID (public)")
    public ResponseEntity<ApiResponse<ModuleResponse>> getModuleById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.success(
                "Module fetched successfully",
                moduleService.getById(id)
        ));
    }

    // ─────────────────────────────────────────
    // INSTRUCTOR ENDPOINTS
    // ─────────────────────────────────────────

    @GetMapping(ApiRoutes.Module.INSTRUCTOR_BASE + ApiRoutes.Module.GET_BY_COURSE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — list ALL modules of own course (including unpublished)")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> getMyModulesByCourse(
            @PathVariable UUID courseId,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Modules fetched successfully",
                moduleService.getModulesByCourseForInstructor(
                        courseId, user.getUsername())
        ));
    }

    @PostMapping(ApiRoutes.Module.INSTRUCTOR_BASE + ApiRoutes.Module.CREATE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — create a new module in own course (DRAFT/REJECTED only)")
    public ResponseEntity<ApiResponse<ModuleResponse>> createModule(
            @PathVariable UUID courseId,
            @Valid @RequestBody ModuleRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        "Module created successfully",
                        moduleService.createModule(
                                courseId, request, user.getUsername())
                )
        );
    }

    @PutMapping(ApiRoutes.Module.INSTRUCTOR_BASE + ApiRoutes.Module.UPDATE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — update module in own course (DRAFT/REJECTED only)")
    public ResponseEntity<ApiResponse<ModuleResponse>> updateModule(
            @PathVariable UUID id,
            @Valid @RequestBody ModuleRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Module updated successfully",
                moduleService.updateModule(id, request, user.getUsername())
        ));
    }

    @PatchMapping(ApiRoutes.Module.INSTRUCTOR_BASE + ApiRoutes.Module.TOGGLE_PUBLISH)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — toggle module publish status")
    public ResponseEntity<ApiResponse<ModuleResponse>> togglePublish(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Module publish status toggled",
                moduleService.togglePublish(id, user.getUsername())
        ));
    }

    @DeleteMapping(ApiRoutes.Module.INSTRUCTOR_BASE + ApiRoutes.Module.DELETE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — delete module from own course (not PUBLISHED)")
    public ResponseEntity<ApiResponse<Void>> deleteModule(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        moduleService.deleteModule(id, user.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success("Module deleted successfully"));
    }

    @PatchMapping(ApiRoutes.Module.INSTRUCTOR_BASE + ApiRoutes.Module.REORDER)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — reorder modules within own course")
    public ResponseEntity<ApiResponse<Void>> reorderModules(
            @Valid @RequestBody ModuleReorderRequest request,
            @CurrentUser UserDetails user) {

        moduleService.reorderModules(request, user.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success("Modules reordered successfully"));
    }

    // ─────────────────────────────────────────
    // ADMIN ENDPOINTS
    // ─────────────────────────────────────────

    @GetMapping(ApiRoutes.Module.ADMIN_BASE + ApiRoutes.Module.ADMIN_GET_BY_COURSE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — list ALL modules of any course (including unpublished)")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> adminGetModulesByCourse(
            @PathVariable UUID courseId) {

        return ResponseEntity.ok(ApiResponse.success(
                "Modules fetched successfully (admin view)",
                moduleService.adminGetModulesByCourse(courseId)
        ));
    }

    @PatchMapping(ApiRoutes.Module.ADMIN_BASE + ApiRoutes.Module.ADMIN_TOGGLE_PUBLISH)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — toggle publish status of any module")
    public ResponseEntity<ApiResponse<ModuleResponse>> adminTogglePublish(
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.success(
                "Module publish status toggled by admin",
                moduleService.adminTogglePublish(id)
        ));
    }

    @DeleteMapping(ApiRoutes.Module.ADMIN_BASE + ApiRoutes.Module.ADMIN_DELETE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — delete any module from any course")
    public ResponseEntity<ApiResponse<Void>> adminDeleteModule(
            @PathVariable UUID id) {

        moduleService.adminDeleteModule(id);

        return ResponseEntity.ok(
                ApiResponse.success("Module deleted by admin"));
    }
}