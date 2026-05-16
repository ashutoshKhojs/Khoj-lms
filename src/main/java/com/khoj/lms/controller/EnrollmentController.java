package com.khoj.lms.controller;

import com.khoj.lms.dto.common.ApiResponse;
import com.khoj.lms.dto.enrollment.*;
import com.khoj.lms.enums.EnrollmentStatus;
import com.khoj.lms.service.EnrollmentService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Course enrollment — student, instructor, admin")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // ═══════════════════════════════════════════════════════════
    // STUDENT
    // ═══════════════════════════════════════════════════════════


    @DeleteMapping(ApiRoutes.Enrollment.BASE + ApiRoutes.Enrollment.UNENROLL)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Unenroll from a course (only if 0 lessons completed)")
    public ResponseEntity<ApiResponse<Void>> unenroll(
            @PathVariable UUID courseId,
            @CurrentUser UserDetails user) {

        enrollmentService.unenroll(courseId, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Unenrolled successfully"));
    }

    @GetMapping(ApiRoutes.Enrollment.BASE + ApiRoutes.Enrollment.MY_LIST)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "List the current user's enrollments")
    public ResponseEntity<ApiResponse<Page<EnrollmentSummary>>> myEnrollments(
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser UserDetails user) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(ApiResponse.success(
                "Enrollments fetched successfully",
                enrollmentService.getMyEnrollments(user.getUsername(), status, pageable)
        ));
    }

    @GetMapping(ApiRoutes.Enrollment.BASE + ApiRoutes.Enrollment.MY_DASHBOARD)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Get my enrollment dashboard for one course")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> myEnrollmentForCourse(
            @PathVariable UUID courseId,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Enrollment fetched successfully",
                enrollmentService.getMyEnrollmentForCourse(courseId, user.getUsername())
        ));
    }

    @GetMapping(ApiRoutes.Enrollment.BASE + ApiRoutes.Enrollment.CONTINUE_LEARNING)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Continue Learning — most recently accessed active courses")
    public ResponseEntity<ApiResponse<List<EnrollmentSummary>>> continueLearning(
            @RequestParam(defaultValue = "5") int limit,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Continue Learning fetched successfully",
                enrollmentService.getContinueLearning(user.getUsername(), limit)
        ));
    }

    @PostMapping(ApiRoutes.Enrollment.BASE + ApiRoutes.Enrollment.REVIEW)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Rate and review a course (only after completion)")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> reviewCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseReviewRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Review submitted successfully",
                enrollmentService.reviewCourse(courseId, request, user.getUsername())
        ));
    }

    // ═══════════════════════════════════════════════════════════
    // INSTRUCTOR
    // ═══════════════════════════════════════════════════════════

    @GetMapping(ApiRoutes.Enrollment.INSTRUCTOR_BASE + ApiRoutes.Enrollment.INSTRUCTOR_BY_COURSE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — list enrollments in own course")
    public ResponseEntity<ApiResponse<Page<EnrollmentSummary>>> instructorListByCourse(
            @PathVariable UUID courseId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser UserDetails user) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(ApiResponse.success(
                "Course enrollments fetched successfully",
                enrollmentService.getCourseEnrollmentsForInstructor(
                        courseId, user.getUsername(), pageable)
        ));
    }

    @GetMapping(ApiRoutes.Enrollment.INSTRUCTOR_BASE + ApiRoutes.Enrollment.INSTRUCTOR_STATS)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Instructor — enrollment & completion stats for own course")
    public ResponseEntity<ApiResponse<CourseEnrollmentStats>> instructorStats(
            @PathVariable UUID courseId,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Stats fetched successfully",
                enrollmentService.getCourseStatsForInstructor(courseId, user.getUsername())
        ));
    }

    // ═══════════════════════════════════════════════════════════
    // ADMIN
    // ═══════════════════════════════════════════════════════════

    @GetMapping(ApiRoutes.Enrollment.ADMIN_BASE + ApiRoutes.Enrollment.ADMIN_LIST)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — list all enrollments")
    public ResponseEntity<ApiResponse<Page<EnrollmentSummary>>> adminListAll(
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(ApiResponse.success(
                "Enrollments fetched (admin view)",
                enrollmentService.adminListAll(status, pageable)
        ));
    }

    @GetMapping(ApiRoutes.Enrollment.ADMIN_BASE + ApiRoutes.Enrollment.ADMIN_BY_COURSE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — list enrollments for any course")
    public ResponseEntity<ApiResponse<Page<EnrollmentSummary>>> adminListByCourse(
            @PathVariable UUID courseId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(ApiResponse.success(
                "Course enrollments fetched (admin view)",
                enrollmentService.adminListByCourse(courseId, pageable)
        ));
    }

    @GetMapping(ApiRoutes.Enrollment.ADMIN_BASE + ApiRoutes.Enrollment.ADMIN_BY_STUDENT)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — list all enrollments of a specific student")
    public ResponseEntity<ApiResponse<Page<EnrollmentSummary>>> adminListByStudent(
            @PathVariable UUID studentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(ApiResponse.success(
                "Student enrollments fetched (admin view)",
                enrollmentService.adminListByStudent(studentId, pageable)
        ));
    }

    @GetMapping(ApiRoutes.Enrollment.ADMIN_BASE + ApiRoutes.Enrollment.ADMIN_STATS)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — enrollment & completion stats for any course")
    public ResponseEntity<ApiResponse<CourseEnrollmentStats>> adminStats(
            @PathVariable UUID courseId) {

        return ResponseEntity.ok(ApiResponse.success(
                "Stats fetched (admin view)",
                enrollmentService.adminGetCourseStats(courseId)
        ));
    }
}