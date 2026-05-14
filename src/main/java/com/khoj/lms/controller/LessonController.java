package com.khoj.lms.controller;

import com.khoj.lms.dto.common.ApiResponse;
import com.khoj.lms.dto.lesson.*;
import com.khoj.lms.service.LessonService;
import com.khoj.lms.util.ApiRoutes;
import com.khoj.lms.util.CurrentUser;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@RequiredArgsConstructor
@Tag(name = "Lessons", description = "Lesson management")
public class LessonController {

    private final LessonService lessonService;

    @GetMapping(ApiRoutes.Lesson.BASE + ApiRoutes.Lesson.GET_BY_MODULE)
    public ResponseEntity<ApiResponse<List<LessonSummary>>> getLessons(
            @PathVariable UUID moduleId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lessons fetched",
                        lessonService.getLessonsByModule(moduleId)
                )
        );
    }

    @GetMapping(ApiRoutes.Lesson.BASE + ApiRoutes.Lesson.GET_BY_ID)
    public ResponseEntity<ApiResponse<LessonResponse>> getLesson(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lesson fetched",
                        lessonService.getLessonById(id)
                )
        );
    }

    @PostMapping(ApiRoutes.Lesson.INSTRUCTOR_BASE + ApiRoutes.Lesson.CREATE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LessonResponse>> createLesson(
            @PathVariable UUID moduleId,
            @Valid @RequestBody LessonRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        "Lesson created",
                        lessonService.createLesson(moduleId, request, user.getUsername())
                )
        );
    }

    @PutMapping(ApiRoutes.Lesson.INSTRUCTOR_BASE + ApiRoutes.Lesson.UPDATE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(
            @PathVariable UUID id,
            @Valid @RequestBody LessonRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lesson updated",
                        lessonService.updateLesson(id, request, user.getUsername())
                )
        );
    }

    @PatchMapping(ApiRoutes.Lesson.INSTRUCTOR_BASE + ApiRoutes.Lesson.TOGGLE_PUBLISH)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LessonResponse>> togglePublish(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lesson publish toggled",
                        lessonService.togglePublish(id, user.getUsername())
                )
        );
    }

    @DeleteMapping(ApiRoutes.Lesson.INSTRUCTOR_BASE + ApiRoutes.Lesson.DELETE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        lessonService.deleteLesson(id, user.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success("Lesson deleted")
        );
    }

    @PatchMapping(ApiRoutes.Lesson.INSTRUCTOR_BASE + ApiRoutes.Lesson.REORDER)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reorderLessons(
            @Valid @RequestBody LessonReorderRequest request,
            @CurrentUser UserDetails user) {

        lessonService.reorderLessons(request, user.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success("Lessons reordered")
        );
    }
}