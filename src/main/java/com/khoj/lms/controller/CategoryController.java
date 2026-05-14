package com.khoj.lms.controller;

import com.khoj.lms.dto.category.*;
import com.khoj.lms.dto.common.ApiResponse;
import com.khoj.lms.service.CategoryService;
import com.khoj.lms.util.ApiRoutes;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;
import java.util.UUID;

/**
 * GET  /categories          — public
 * GET  /categories/{id}     — public
 * POST /admin/categories    — ADMIN only
 * PUT  /admin/categories/{id}    — ADMIN only
 * DELETE /admin/categories/{id} — ADMIN only
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Course category management")
public class CategoryController {

    private final CategoryService categoryService;

    // PUBLIC

    @GetMapping(ApiRoutes.Category.BASE + ApiRoutes.Category.LIST)
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Categories fetched successfully",
                        categoryService.getAllWithChildren()
                )
        );
    }

    @GetMapping(ApiRoutes.Category.BASE + ApiRoutes.Category.GET_BY_ID)
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Category fetched successfully",
                        categoryService.getById(id)
                )
        );
    }

    // ADMIN

    @PostMapping(ApiRoutes.Category.ADMIN_BASE + ApiRoutes.Category.CREATE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CategoryRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Category created successfully",
                                categoryService.create(request)
                        )
                );
    }

    @PutMapping(ApiRoutes.Category.ADMIN_BASE + ApiRoutes.Category.UPDATE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Category updated successfully",
                        categoryService.update(id, request)
                )
        );
    }

    @DeleteMapping(ApiRoutes.Category.ADMIN_BASE + ApiRoutes.Category.DELETE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {

        categoryService.delete(id);

        return ResponseEntity.ok(
                ApiResponse.success("Category deleted successfully")
        );
    }
}