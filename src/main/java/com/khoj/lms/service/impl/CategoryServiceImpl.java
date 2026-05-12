package com.khoj.lms.service.impl;

import com.khoj.lms.audit.AuditLogger;
import com.khoj.lms.dto.category.*;
import com.khoj.lms.entity.Category;
import com.khoj.lms.exception.BadRequestException;
import com.khoj.lms.exception.ResourceNotFoundException;
import com.khoj.lms.repository.CategoryRepository;
import com.khoj.lms.service.CategoryService;
import com.khoj.lms.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuditLogger        auditLogger;

    // ================= PUBLIC =================

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllWithChildren() {
        log.debug("Fetching all root categories with children");

        List<Category> roots = categoryRepository.findRootCategories();

        log.debug("Found {} root categories", roots.size());

        return roots.stream()
                .map(this::toResponseWithChildren)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID id) {
        log.debug("Fetching category by id={}", id);

        Category category = findOrThrow(id);
        return toResponseWithChildren(category);
    }

    // ================= ADMIN =================

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        log.info("Creating category: name='{}'", request.getName());

        String baseSlug = SlugUtil.toSlug(request.getName());
        String slug = SlugUtil.makeUnique(baseSlug,
                s -> categoryRepository.existsBySlugAndIsDeletedFalse(s));

        Category parent = null;

        if (request.getParentId() != null) {
            log.debug("Resolving parent category id={}", request.getParentId());

            parent = findOrThrow(request.getParentId());

            if (parent.getParent() != null) {
                log.warn("Attempted to create nested sub-category under id={}",
                        request.getParentId());
                throw new BadRequestException(
                        "Only one level of sub-categories is supported.");
            }
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .displayOrder(request.getDisplayOrder() != null
                        ? request.getDisplayOrder() : 0)
                .parent(parent)
                .isActive(true)
                .build();

        category = categoryRepository.save(category);

        log.info("Category created: id={} name='{}' slug='{}'",
                category.getId(), category.getName(), category.getSlug());

        auditLogger.adminAction(
                "system",
                "CATEGORY_CREATED: " + category.getName(),
                null
        );

        return toResponseWithChildren(category);
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        log.info("Updating category id={} name='{}'", id, request.getName());

        Category category = findOrThrow(id);
        String oldName = category.getName();

        if (!category.getName().equalsIgnoreCase(request.getName())) {
            log.debug("Category name changed '{}' → '{}', regenerating slug",
                    oldName, request.getName());

            String baseSlug = SlugUtil.toSlug(request.getName());
            String newSlug = SlugUtil.makeUnique(baseSlug,
                    s -> !s.equals(category.getSlug())
                            && categoryRepository.existsBySlugAndIsDeletedFalse(s));

            category.setSlug(newSlug);

            log.debug("New slug generated: '{}'", newSlug);
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIconUrl(request.getIconUrl());

        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }

        Category saved = categoryRepository.save(category);

        log.info("Category updated: id={} oldName='{}' newName='{}'",
                id, oldName, saved.getName());

        auditLogger.adminAction(
                "system",
                "CATEGORY_UPDATED: " + saved.getName(),
                null
        );

        return toResponseWithChildren(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        log.info("Attempting to delete category id={}", id);

        Category category = findOrThrow(id);

        if (!category.getCourses().isEmpty()) {
            log.warn("Delete blocked — category id={} name='{}' has {} courses",
                    id, category.getName(), category.getCourses().size());

            throw new BadRequestException(
                    "Cannot delete category with existing courses. " +
                            "Reassign courses first.");
        }

        category.softDelete();
        categoryRepository.save(category);

        log.info("Category soft-deleted: id={} name='{}'",
                id, category.getName());

        auditLogger.adminAction(
                "system",
                "CATEGORY_DELETED: " + category.getName(),
                null
        );
    }

    // ================= HELPERS =================

    private Category findOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category", "id", id));
    }

    // ================= MAPPER =================

    private CategoryResponse toResponseWithChildren(Category c) {
        List<CategoryResponse> children = c.getChildren().stream()
                .filter(ch -> !ch.getIsDeleted())
                .map(ch -> CategoryResponse.builder()
                        .id(ch.getId())
                        .name(ch.getName())
                        .slug(ch.getSlug())
                        .description(ch.getDescription())
                        .iconUrl(ch.getIconUrl())
                        .displayOrder(ch.getDisplayOrder())
                        .parentId(c.getId())
                        .parentName(c.getName())
                        .build())
                .collect(Collectors.toList());

        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .description(c.getDescription())
                .iconUrl(c.getIconUrl())
                .displayOrder(c.getDisplayOrder())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .parentName(c.getParent() != null ? c.getParent().getName() : null)
                .courseCount(c.getCourses().size())
                .children(children)
                .build();
    }
}