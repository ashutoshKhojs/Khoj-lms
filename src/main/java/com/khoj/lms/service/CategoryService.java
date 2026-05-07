package com.khoj.lms.service;

import com.khoj.lms.dto.category.CategoryRequest;
import com.khoj.lms.dto.category.CategoryResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    // PUBLIC
    List<CategoryResponse> getAllWithChildren();

    CategoryResponse getById(UUID id);

    // ADMIN
    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(UUID id, CategoryRequest request);

    void delete(UUID id);
}