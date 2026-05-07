package com.khoj.lms.repository;

import com.khoj.lms.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlugAndIsDeletedFalse(String slug);

    boolean existsBySlugAndIsDeletedFalse(String slug);

    /** Root categories ordered by displayOrder */
    @Query("""
        SELECT c FROM Category c
        WHERE c.parent IS NULL
          AND c.isDeleted = false
          AND c.isActive = true
        ORDER BY c.displayOrder ASC
        """)
    List<Category> findRootCategories();

    List<Category> findByParentIdAndIsDeletedFalseOrderByDisplayOrderAsc(UUID parentId);
}