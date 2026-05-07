package com.khoj.lms.repository;

import com.khoj.lms.entity.InstructorApplication;
import com.khoj.lms.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface InstructorApplicationRepository extends JpaRepository<InstructorApplication, UUID> {

    boolean existsByUserIdAndIsDeletedFalse(UUID userId);

    boolean existsByUserIdAndStatusAndIsDeletedFalse(UUID userId, ApplicationStatus status);

    Optional<InstructorApplication> findByUserIdAndIsDeletedFalse(UUID userId);

    Page<InstructorApplication> findByStatusAndIsDeletedFalse(ApplicationStatus status, Pageable pageable);

    @Query("SELECT a FROM InstructorApplication a JOIN FETCH a.user WHERE a.id = :id AND a.isDeleted = false")
    Optional<InstructorApplication> findByIdWithUser(UUID id);
}