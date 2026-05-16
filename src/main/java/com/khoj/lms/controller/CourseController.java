package com.khoj.lms.controller;

import com.khoj.lms.dto.common.ApiResponse;
import com.khoj.lms.dto.course.*;
import com.khoj.lms.enums.CourseStatus;
import com.khoj.lms.enums.DifficultyLevel;
import com.khoj.lms.service.CourseService;
import com.khoj.lms.util.ApiRoutes;
import com.khoj.lms.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    // PUBLIC ENDPOINTS
    // ─────────────────────────────────────────

    @GetMapping(ApiRoutes.Course.BASE + ApiRoutes.Course.LIST)
    @Operation(summary = "List all published courses with filters and pagination")
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
                        ? DifficultyLevel.valueOf(difficulty.toUpperCase())
                        : null)
                .language(language)
                .isFree(isFree)
                .build();

        Sort sortObj = direction.equalsIgnoreCase("asc")
                ? Sort.by(sort).ascending()
                : Sort.by(sort).descending();

        return ResponseEntity.ok(ApiResponse.success(
                "Courses fetched successfully",
                courseService.getPublishedCourses(
                        filter, PageRequest.of(page, size, sortObj))
        ));
    }

    @GetMapping(ApiRoutes.Course.BASE + ApiRoutes.Course.GET_BY_SLUG)
    @Operation(summary = "Get full course detail by slug (public)")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseBySlug(
            @PathVariable String slug) {

        return ResponseEntity.ok(ApiResponse.success(
                "Course fetched successfully",
                courseService.getCourseBySlug(slug)
        ));
    }

    // ─────────────────────────────────────────
    // INSTRUCTOR ENDPOINTS
    // ─────────────────────────────────────────

    @GetMapping(ApiRoutes.Course.INSTRUCTOR_BASE + ApiRoutes.Course.INSTRUCTOR_LIST)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — list own courses (all statuses)")
    public ResponseEntity<ApiResponse<Page<CourseResponse>>> getMyCoursesAsInstructor(
            @CurrentUser UserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(ApiResponse.success(
                "Instructor courses fetched",
                courseService.getInstructorCourses(
                        user.getUsername(),
                        PageRequest.of(page, size,
                                Sort.by("createdAt").descending()))
        ));
    }

    @GetMapping(ApiRoutes.Course.INSTRUCTOR_BASE + ApiRoutes.Course.INSTRUCTOR_GET)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — get own course detail by ID")
    public ResponseEntity<ApiResponse<CourseResponse>> getInstructorCourseById(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Course fetched successfully",
                courseService.getInstructorCourseById(id, user.getUsername())
        ));
    }

    @PostMapping(ApiRoutes.Course.INSTRUCTOR_BASE + ApiRoutes.Course.CREATE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — create a new course (starts as DRAFT)")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CourseRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Course created successfully",
                        courseService.createCourse(request, user.getUsername())
                ));
    }

    @PutMapping(ApiRoutes.Course.INSTRUCTOR_BASE + ApiRoutes.Course.UPDATE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — update own course (only DRAFT or REJECTED)")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable UUID id,
            @Valid @RequestBody CourseRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Course updated successfully",
                courseService.updateCourse(id, request, user.getUsername())
        ));
    }

    @PostMapping(ApiRoutes.Course.INSTRUCTOR_BASE + ApiRoutes.Course.SUBMIT)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — submit course for admin review (DRAFT → PENDING)")
    public ResponseEntity<ApiResponse<CourseResponse>> submitForReview(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Course submitted for review. You will be notified once reviewed.",
                courseService.submitForReview(id, user.getUsername())
        ));
    }

    @DeleteMapping(ApiRoutes.Course.INSTRUCTOR_BASE + ApiRoutes.Course.DELETE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — delete own course (only non-published, no enrolled students)")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        courseService.deleteCourse(id, user.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success("Course deleted successfully"));
    }

    @PatchMapping(ApiRoutes.Course.INSTRUCTOR_BASE + ApiRoutes.Course.PUBLISHED_UPDATE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(
            summary = "Update cosmetic fields of a PUBLISHED course",
            description = "Allowed: thumbnail, descriptions, prerequisites, target audience, tags, preview video. " +
                    "Locked: title, slug, price, isFree, category, difficulty, certificate settings."
    )
    public ResponseEntity<ApiResponse<CourseResponse>> updatePublishedCourse(
            @PathVariable UUID id,
            @Valid @RequestBody PublishedCourseUpdateRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Course updated successfully",
                courseService.updatePublishedCourse(id, request, user.getUsername())
        ));
    }

    // ─────────────────────────────────────────
    // ADMIN ENDPOINTS
    // ─────────────────────────────────────────

    @GetMapping(ApiRoutes.Course.ADMIN_BASE + ApiRoutes.Course.ADMIN_LIST)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — list all courses with optional status filter")
    public ResponseEntity<ApiResponse<Page<CourseSummary>>> getAllCoursesAdmin(
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sortObj = direction.equalsIgnoreCase("asc")
                ? Sort.by(sort).ascending()
                : Sort.by(sort).descending();

        return ResponseEntity.ok(ApiResponse.success(
                "Admin courses fetched",
                courseService.getAllCoursesForAdmin(
                        status, PageRequest.of(page, size, sortObj))
        ));
    }

    @GetMapping(ApiRoutes.Course.ADMIN_BASE + ApiRoutes.Course.ADMIN_GET)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — get any course detail by ID")
    public ResponseEntity<ApiResponse<CourseResponse>> getAdminCourseById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.success(
                "Course fetched successfully",
                courseService.getAdminCourseById(id)
        ));
    }

    @PatchMapping(ApiRoutes.Course.ADMIN_BASE + ApiRoutes.Course.APPROVE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — approve a PENDING course (→ PUBLISHED)")
    public ResponseEntity<ApiResponse<CourseResponse>> approveCourse(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Course approved and published successfully",
                courseService.approveCourse(id, user.getUsername())
        ));
    }

    @PatchMapping(ApiRoutes.Course.ADMIN_BASE + ApiRoutes.Course.REJECT)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — reject a PENDING course with reason (→ REJECTED)")
    public ResponseEntity<ApiResponse<CourseResponse>> rejectCourse(
            @PathVariable UUID id,
            @Valid @RequestBody CourseRejectionRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Course rejected. Instructor will be notified.",
                courseService.rejectCourse(id, request, user.getUsername())
        ));
    }

    @PatchMapping(ApiRoutes.Course.ADMIN_BASE + ApiRoutes.Course.ARCHIVE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — archive a PUBLISHED course (→ ARCHIVED)")
    public ResponseEntity<ApiResponse<CourseResponse>> archiveCourse(
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.success(
                "Course archived successfully",
                courseService.archiveCourse(id)
        ));
    }
}