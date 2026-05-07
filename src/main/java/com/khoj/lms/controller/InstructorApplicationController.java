package com.khoj.lms.controller;

import com.khoj.lms.dto.common.ApiResponse;
import com.khoj.lms.dto.instructor.InstructorApplicationDtos.*;
import com.khoj.lms.enums.ApplicationStatus;
import com.khoj.lms.service.InstructorApplicationService;
import com.khoj.lms.util.ApiRoutes;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Instructor Applications", description = "Apply to become an instructor")
public class InstructorApplicationController {

    private final InstructorApplicationService applicationService;

    // ─────────────────────────────────────────
    // STUDENT APIs
    // ─────────────────────────────────────────

    @PostMapping(ApiRoutes.Instructor.BASE + ApiRoutes.Instructor.APPLY)
    public ResponseEntity<ApiResponse<ApplicationResponse>> apply(
            @Valid @RequestBody ApplyRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Application submitted successfully",
                        applicationService.apply(request, userDetails.getUsername())
                )
        );
    }

    @GetMapping(ApiRoutes.Instructor.BASE + ApiRoutes.Instructor.MY_APPLICATION)
    public ResponseEntity<ApiResponse<ApplicationResponse>> getMyApplication(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Application fetched",
                        applicationService.getMyApplication(userDetails.getUsername())
                )
        );
    }

    // ─────────────────────────────────────────
    // ADMIN APIs
    // ─────────────────────────────────────────

    @GetMapping(ApiRoutes.Instructor.ADMIN_BASE + ApiRoutes.Instructor.LIST)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> listApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ApplicationResponse> applications = applicationService.listApplications(
                status,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        return ResponseEntity.ok(
                ApiResponse.success("Applications fetched", applications)
        );
    }

    @PatchMapping(ApiRoutes.Instructor.ADMIN_BASE + ApiRoutes.Instructor.APPROVE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Application approved. User is now an instructor.",
                        applicationService.approve(id, userDetails.getUsername())
                )
        );
    }

    @PatchMapping(ApiRoutes.Instructor.ADMIN_BASE + ApiRoutes.Instructor.REJECT)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Application rejected.",
                        applicationService.reject(id, request, userDetails.getUsername())
                )
        );
    }
}