package com.khoj.lms.controller;

import com.khoj.lms.dto.common.ApiResponse;
import com.khoj.lms.dto.lesson.*;
import com.khoj.lms.service.LessonService;
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
@Tag(name = "Lessons", description = "Lesson management — public viewing, instructor authoring, admin control")
public class LessonController {

    private final LessonService lessonService;

    // ═══════════════════════════════════════════════════════════
    // PUBLIC ENDPOINTS (only PUBLISHED lessons of PUBLISHED courses)
    // ═══════════════════════════════════════════════════════════

    @GetMapping(ApiRoutes.Lesson.BASE + ApiRoutes.Lesson.GET_BY_MODULE)
    @Operation(summary = "List published lessons of a published module (public)")
    public ResponseEntity<ApiResponse<List<LessonSummary>>> getLessonsByModule(
            @PathVariable UUID moduleId) {

        return ResponseEntity.ok(ApiResponse.success(
                "Lessons fetched successfully",
                lessonService.getLessonsByModule(moduleId)
        ));
    }

    @GetMapping(ApiRoutes.Lesson.BASE + ApiRoutes.Lesson.GET_BY_COURSE)
    @Operation(summary = "List all published lessons of a published course (public)")
    public ResponseEntity<ApiResponse<List<LessonSummary>>> getLessonsByCourse(
            @PathVariable UUID courseId) {

        return ResponseEntity.ok(ApiResponse.success(
                "Lessons fetched successfully",
                lessonService.getLessonsByCourse(courseId)
        ));
    }

    @GetMapping(ApiRoutes.Lesson.BASE + ApiRoutes.Lesson.GET_BY_ID)
    @Operation(summary = "Get a single lesson by ID (public — only published or preview)")
    public ResponseEntity<ApiResponse<LessonResponse>> getLesson(
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.success(
                "Lesson fetched successfully",
                lessonService.getLessonById(id)
        ));
    }

    // ═══════════════════════════════════════════════════════════
    // INSTRUCTOR ENDPOINTS
    // ═══════════════════════════════════════════════════════════

    @GetMapping(ApiRoutes.Lesson.INSTRUCTOR_BASE + ApiRoutes.Lesson.INSTRUCTOR_BY_MODULE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — list ALL lessons of a module (including drafts)")
    public ResponseEntity<ApiResponse<List<LessonSummary>>> getMyLessonsByModule(
            @PathVariable UUID moduleId,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Lessons fetched successfully",
                lessonService.getLessonsByModuleForInstructor(moduleId, user.getUsername())
        ));
    }

    @GetMapping(ApiRoutes.Lesson.INSTRUCTOR_BASE + ApiRoutes.Lesson.INSTRUCTOR_BY_COURSE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — list ALL lessons of own course (including drafts)")
    public ResponseEntity<ApiResponse<List<LessonSummary>>> getMyLessonsByCourse(
            @PathVariable UUID courseId,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Lessons fetched successfully",
                lessonService.getLessonsByCourseForInstructor(courseId, user.getUsername())
        ));
    }

    @GetMapping(ApiRoutes.Lesson.INSTRUCTOR_BASE + ApiRoutes.Lesson.INSTRUCTOR_GET)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — get own lesson by ID (any status)")
    public ResponseEntity<ApiResponse<LessonResponse>> getMyLesson(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Lesson fetched successfully",
                lessonService.getLessonByIdForInstructor(id, user.getUsername())
        ));
    }

    @PostMapping(ApiRoutes.Lesson.INSTRUCTOR_BASE + ApiRoutes.Lesson.CREATE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — create a new lesson in own module")
    public ResponseEntity<ApiResponse<LessonResponse>> createLesson(
            @PathVariable UUID moduleId,
            @Valid @RequestBody LessonRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Lesson created successfully",
                lessonService.createLesson(moduleId, request, user.getUsername())
        ));
    }

    @PutMapping(ApiRoutes.Lesson.INSTRUCTOR_BASE + ApiRoutes.Lesson.UPDATE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — update own lesson (only DRAFT/REJECTED courses)")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(
            @PathVariable UUID id,
            @Valid @RequestBody LessonRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Lesson updated successfully",
                lessonService.updateLesson(id, request, user.getUsername())
        ));
    }

    @PatchMapping(ApiRoutes.Lesson.INSTRUCTOR_BASE + ApiRoutes.Lesson.TOGGLE_PUBLISH)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — toggle lesson publish status")
    public ResponseEntity<ApiResponse<LessonResponse>> togglePublish(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Lesson publish status toggled",
                lessonService.togglePublish(id, user.getUsername())
        ));
    }

    @DeleteMapping(ApiRoutes.Lesson.INSTRUCTOR_BASE + ApiRoutes.Lesson.DELETE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — delete own lesson (only DRAFT/REJECTED courses)")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        lessonService.deleteLesson(id, user.getUsername());

        return ResponseEntity.ok(ApiResponse.success("Lesson deleted successfully"));
    }

    @PatchMapping(ApiRoutes.Lesson.INSTRUCTOR_BASE + ApiRoutes.Lesson.REORDER)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — reorder lessons within own module")
    public ResponseEntity<ApiResponse<Void>> reorderLessons(
            @Valid @RequestBody LessonReorderRequest request,
            @CurrentUser UserDetails user) {

        lessonService.reorderLessons(request, user.getUsername());

        return ResponseEntity.ok(ApiResponse.success("Lessons reordered successfully"));
    }

    // ═══════════════════════════════════════════════════════════
    // ADMIN ENDPOINTS
    // ═══════════════════════════════════════════════════════════

    @GetMapping(ApiRoutes.Lesson.ADMIN_BASE + ApiRoutes.Lesson.ADMIN_BY_MODULE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — list ALL lessons of any module (incl. drafts)")
    public ResponseEntity<ApiResponse<List<LessonSummary>>> adminGetLessonsByModule(
            @PathVariable UUID moduleId) {

        return ResponseEntity.ok(ApiResponse.success(
                "Lessons fetched (admin view)",
                lessonService.adminGetLessonsByModule(moduleId)
        ));
    }

    @GetMapping(ApiRoutes.Lesson.ADMIN_BASE + ApiRoutes.Lesson.ADMIN_BY_COURSE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — list ALL lessons of any course (incl. drafts)")
    public ResponseEntity<ApiResponse<List<LessonSummary>>> adminGetLessonsByCourse(
            @PathVariable UUID courseId) {

        return ResponseEntity.ok(ApiResponse.success(
                "Lessons fetched (admin view)",
                lessonService.adminGetLessonsByCourse(courseId)
        ));
    }

    @GetMapping(ApiRoutes.Lesson.ADMIN_BASE + ApiRoutes.Lesson.ADMIN_GET)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — get any lesson by ID")
    public ResponseEntity<ApiResponse<LessonResponse>> adminGetLesson(
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.success(
                "Lesson fetched (admin view)",
                lessonService.adminGetLessonById(id)
        ));
    }

    @PatchMapping(ApiRoutes.Lesson.ADMIN_BASE + ApiRoutes.Lesson.ADMIN_TOGGLE_PUBLISH)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — toggle publish status of any lesson")
    public ResponseEntity<ApiResponse<LessonResponse>> adminTogglePublish(
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.success(
                "Lesson publish toggled by admin",
                lessonService.adminTogglePublish(id)
        ));
    }

    @DeleteMapping(ApiRoutes.Lesson.ADMIN_BASE + ApiRoutes.Lesson.ADMIN_DELETE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — delete any lesson from any course")
    public ResponseEntity<ApiResponse<Void>> adminDeleteLesson(
            @PathVariable UUID id) {

        lessonService.adminDeleteLesson(id);

        return ResponseEntity.ok(ApiResponse.success("Lesson deleted by admin"));
    }
}