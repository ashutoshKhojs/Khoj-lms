package com.khoj.lms.controller;

import com.khoj.lms.dto.common.ApiResponse;
import com.khoj.lms.dto.course.*;
import com.khoj.lms.enums.CourseStatus;
import com.khoj.lms.service.CourseService;
import com.khoj.lms.util.ApiRoutes;
import com.khoj.lms.util.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course management — public browsing, instructor authoring, admin approval")
public class CourseController {

    private final CourseService courseService;

    // ─────────────────────────────────────────
    // PUBLIC
    // ─────────────────────────────────────────

    @GetMapping(ApiRoutes.Course.BASE + ApiRoutes.Course.LIST)
    public ResponseEntity<ApiResponse<Page<CourseSummary>>> getCourses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "enrolledCount") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        CourseFilter filter = CourseFilter.builder()
                .search(search)
                .categoryId(categoryId)
                .difficulty(difficulty != null
                        ? com.khoj.lms.enums.DifficultyLevel.valueOf(difficulty.toUpperCase())
                        : null)
                .language(language)
                .isFree(isFree)
                .build();

        Sort sortObj = direction.equalsIgnoreCase("desc")
                ? Sort.by(sort).descending()
                : Sort.by(sort).ascending();

        Pageable pageable = PageRequest.of(page, size, sortObj);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Courses fetched successfully",
                        courseService.getPublishedCourses(filter, pageable)
                )
        );
    }

    @GetMapping(ApiRoutes.Course.BASE + ApiRoutes.Course.GET_BY_SLUG)
    public ResponseEntity<ApiResponse<CourseResponse>> getCourse(@PathVariable String slug) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Course fetched successfully",
                        courseService.getCourseBySlug(slug)
                )
        );
    }

    // ─────────────────────────────────────────
    // INSTRUCTOR
    // ─────────────────────────────────────────

    @GetMapping(ApiRoutes.Course.INSTRUCTOR_BASE + ApiRoutes.Course.INSTRUCTOR_LIST)
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<InstructorCourseView>>> getMyCoursesAsInstructor(
            @CurrentUser UserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Instructor courses fetched",
                        courseService.getInstructorCourses(user.getUsername(), pageable)
                )
        );
    }

    @PostMapping(ApiRoutes.Course.INSTRUCTOR_BASE + ApiRoutes.Course.CREATE)
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CourseRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Course created successfully",
                                courseService.createCourse(request, user.getUsername())
                        )
                );
    }

    @PutMapping(ApiRoutes.Course.INSTRUCTOR_BASE + ApiRoutes.Course.UPDATE)
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable UUID id,
            @Valid @RequestBody CourseRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Course updated successfully",
                        courseService.updateCourse(id, request, user.getUsername())
                )
        );
    }

    @PostMapping(ApiRoutes.Course.INSTRUCTOR_BASE + ApiRoutes.Course.SUBMIT)
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> submitForReview(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Course submitted for review",
                        courseService.submitForReview(id, user.getUsername())
                )
        );
    }

    @DeleteMapping(ApiRoutes.Course.INSTRUCTOR_BASE + ApiRoutes.Course.DELETE)
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        courseService.deleteCourse(id, user.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success("Course deleted successfully")
        );
    }

    // ─────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────

    @GetMapping(ApiRoutes.Course.ADMIN_BASE + ApiRoutes.Course.ADMIN_LIST)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<CourseSummary>>> getAllCoursesAdmin(
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Admin courses fetched",
                        courseService.getAllCoursesForAdmin(status, pageable)
                )
        );
    }

    @PatchMapping(ApiRoutes.Course.ADMIN_BASE + ApiRoutes.Course.APPROVE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> approveCourse(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Course approved successfully",
                        courseService.approveCourse(id, user.getUsername())
                )
        );
    }

    @PatchMapping(ApiRoutes.Course.ADMIN_BASE + ApiRoutes.Course.REJECT)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> rejectCourse(
            @PathVariable UUID id,
            @Valid @RequestBody CourseRejectionRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Course rejected",
                        courseService.rejectCourse(id, request, user.getUsername())
                )
        );
    }

    @PatchMapping(ApiRoutes.Course.ADMIN_BASE + ApiRoutes.Course.ARCHIVE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> archiveCourse(@PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Course archived",
                        courseService.archiveCourse(id)
                )
        );
    }
}