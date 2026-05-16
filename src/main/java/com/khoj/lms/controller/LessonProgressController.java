package com.khoj.lms.controller;

import com.khoj.lms.dto.common.ApiResponse;
import com.khoj.lms.dto.progress.*;
import com.khoj.lms.service.LessonProgressService;
import com.khoj.lms.util.ApiRoutes;
import com.khoj.lms.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Lesson Progress", description = "Track student progress through lessons")
public class LessonProgressController {

    private final LessonProgressService progressService;

    // ═══════════════════════════════════════════════════════════
    // STUDENT
    // ═══════════════════════════════════════════════════════════

    @PostMapping(ApiRoutes.Progress.BASE + ApiRoutes.Progress.ACCESS)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Record that the student opened a lesson")
    public ResponseEntity<ApiResponse<LessonProgressResponse>> recordAccess(
            @PathVariable UUID lessonId,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Access recorded",
                progressService.recordAccess(lessonId, user.getUsername())
        ));
    }

    @PatchMapping(ApiRoutes.Progress.BASE + ApiRoutes.Progress.WATCH_POSITION)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Update video watch position (heartbeat) — auto-completes at 90%")
    public ResponseEntity<ApiResponse<LessonProgressResponse>> updateWatchPosition(
            @PathVariable UUID lessonId,
            @Valid @RequestBody WatchPositionRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Watch position updated",
                progressService.updateWatchPosition(lessonId, request, user.getUsername())
        ));
    }

    @PostMapping(ApiRoutes.Progress.BASE + ApiRoutes.Progress.COMPLETE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Manually mark a lesson as completed")
    public ResponseEntity<ApiResponse<LessonProgressResponse>> markCompleted(
            @PathVariable UUID lessonId,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Lesson marked completed",
                progressService.markCompleted(lessonId, user.getUsername())
        ));
    }

    @GetMapping(ApiRoutes.Progress.BASE + ApiRoutes.Progress.GET_LESSON)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Get my progress on a single lesson")
    public ResponseEntity<ApiResponse<LessonProgressResponse>> getMyLessonProgress(
            @PathVariable UUID lessonId,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Lesson progress fetched",
                progressService.getMyLessonProgress(lessonId, user.getUsername())
        ));
    }

    @GetMapping(ApiRoutes.Progress.BASE + ApiRoutes.Progress.GET_COURSE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Get my full progress for one course")
    public ResponseEntity<ApiResponse<CourseProgressSummary>> getMyCourseProgress(
            @PathVariable UUID courseId,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Course progress fetched",
                progressService.getMyCourseProgress(courseId, user.getUsername())
        ));
    }

    // ═══════════════════════════════════════════════════════════
    // ADMIN
    // ═══════════════════════════════════════════════════════════

    @GetMapping(ApiRoutes.Progress.ADMIN_BASE + ApiRoutes.Progress.ADMIN_GET_COURSE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — view any student's progress in any course")
    public ResponseEntity<ApiResponse<CourseProgressSummary>> adminGetCourseProgress(
            @PathVariable UUID courseId,
            @PathVariable UUID studentId) {

        return ResponseEntity.ok(ApiResponse.success(
                "Course progress fetched (admin view)",
                progressService.adminGetCourseProgress(courseId, studentId)
        ));
    }
}