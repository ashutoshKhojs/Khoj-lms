package com.khoj.lms.repository;

import com.khoj.lms.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    boolean existsByEmailAndIsDeletedFalse(String email);

    Optional<User> findByResetTokenAndIsDeletedFalse(String resetToken);

    // ✅ clear naming
    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findWithRolesByEmailAndIsDeletedFalse(String email);
}