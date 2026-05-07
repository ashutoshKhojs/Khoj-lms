package com.khoj.lms.controller;

import com.khoj.lms.dto.common.ApiResponse;
import com.khoj.lms.dto.module.*;
import com.khoj.lms.service.ModuleService;
import com.khoj.lms.util.ApiRoutes;
import com.khoj.lms.util.CurrentUser;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(ApiRoutes.Module.BASE)
@RequiredArgsConstructor
@Tag(name = "Modules", description = "Course module management")
public class ModuleController {

    private final ModuleService moduleService;

    @GetMapping(ApiRoutes.Module.GET_BY_COURSE)
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> getModules(
            @PathVariable UUID courseId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Modules fetched",
                        moduleService.getModulesByCourse(courseId)
                )
        );
    }

    @GetMapping(ApiRoutes.Module.GET_BY_ID)
    public ResponseEntity<ApiResponse<ModuleResponse>> getModule(@PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Module fetched",
                        moduleService.getById(id)
                )
        );
    }

    @PostMapping(ApiRoutes.Module.CREATE)
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModuleResponse>> createModule(
            @PathVariable UUID courseId,
            @Valid @RequestBody ModuleRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        "Module created",
                        moduleService.createModule(courseId, request, user.getUsername())
                )
        );
    }

    @PutMapping(ApiRoutes.Module.UPDATE)
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModuleResponse>> updateModule(
            @PathVariable UUID id,
            @Valid @RequestBody ModuleRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Module updated",
                        moduleService.updateModule(id, request, user.getUsername())
                )
        );
    }

    @PatchMapping(ApiRoutes.Module.TOGGLE_PUBLISH)
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModuleResponse>> togglePublish(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Module publish toggled",
                        moduleService.togglePublish(id, user.getUsername())
                )
        );
    }

    @DeleteMapping(ApiRoutes.Module.DELETE)
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteModule(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        moduleService.deleteModule(id, user.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success("Module deleted")
        );
    }

    @PatchMapping(ApiRoutes.Module.REORDER)
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reorderModules(
            @Valid @RequestBody ModuleReorderRequest request,
            @CurrentUser UserDetails user) {

        moduleService.reorderModules(request, user.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success("Modules reordered")
        );
    }
}