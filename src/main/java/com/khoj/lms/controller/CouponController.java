package com.khoj.lms.controller;

import com.khoj.lms.dto.common.ApiResponse;
import com.khoj.lms.dto.coupon.*;
import com.khoj.lms.service.CouponService;
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
@Tag(name = "Coupons", description = "Discount coupon management & validation")
public class CouponController {

    private final CouponService couponService;

    @GetMapping(ApiRoutes.Coupon.BASE + ApiRoutes.Coupon.PUBLIC_LIST)
    @Operation(summary = "Public — list active promotional coupons (homepage banners)")
    public ResponseEntity<ApiResponse<List<PublicCouponResponse>>> publicList() {

        return ResponseEntity.ok(ApiResponse.success(
                "Public coupons fetched",
                couponService.listPublicActive()
        ));
    }

    @GetMapping(ApiRoutes.Coupon.BASE + ApiRoutes.Coupon.APPLICABLE)
    @Operation(summary = "Public — list coupons applicable to a specific course")
    public ResponseEntity<ApiResponse<List<PublicCouponResponse>>> applicableForCourse(
            @PathVariable UUID courseId) {

        return ResponseEntity.ok(ApiResponse.success(
                "Applicable coupons fetched",
                couponService.listApplicableForCourse(courseId)
        ));
    }

    // ───── STUDENT ─────

    @GetMapping(ApiRoutes.Coupon.BASE + ApiRoutes.Coupon.VALIDATE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Validate a coupon for a course (preview discount before checkout)")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validate(
            @RequestParam String code,
            @RequestParam UUID courseId,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Coupon checked",
                couponService.validate(code, courseId, user.getUsername())
        ));
    }

    // ───── ADMIN ─────

    @PostMapping(ApiRoutes.Coupon.ADMIN_BASE + ApiRoutes.Coupon.ADMIN_CREATE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — create a coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> create(
            @Valid @RequestBody CouponRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Coupon created",
                couponService.create(request, user.getUsername())
        ));
    }

    @PutMapping(ApiRoutes.Coupon.ADMIN_BASE + ApiRoutes.Coupon.ADMIN_UPDATE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — update a coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CouponRequest request,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Coupon updated",
                couponService.update(id, request, user.getUsername())
        ));
    }

    @DeleteMapping(ApiRoutes.Coupon.ADMIN_BASE + ApiRoutes.Coupon.ADMIN_DELETE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — soft-delete a coupon")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {
        couponService.delete(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Coupon deleted"));
    }

    @PatchMapping(ApiRoutes.Coupon.ADMIN_BASE + ApiRoutes.Coupon.ADMIN_TOGGLE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — toggle coupon active flag")
    public ResponseEntity<ApiResponse<CouponResponse>> toggle(
            @PathVariable UUID id,
            @CurrentUser UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
                "Coupon toggled",
                couponService.toggleActive(id, user.getUsername())
        ));
    }

    @GetMapping(ApiRoutes.Coupon.ADMIN_BASE + ApiRoutes.Coupon.ADMIN_LIST)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — list all coupons")
    public ResponseEntity<ApiResponse<Page<CouponResponse>>> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                "Coupons fetched",
                couponService.adminList(active, pageable)
        ));
    }

    @GetMapping(ApiRoutes.Coupon.ADMIN_BASE + ApiRoutes.Coupon.ADMIN_GET)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin — get a coupon by id")
    public ResponseEntity<ApiResponse<CouponResponse>> get(
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.success(
                "Coupon fetched",
                couponService.adminGetById(id)
        ));
    }
}