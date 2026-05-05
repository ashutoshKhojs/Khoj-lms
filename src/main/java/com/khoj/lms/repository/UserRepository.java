package com.khoj.lms.repository;

import com.khoj.lms.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    boolean existsByEmailAndIsDeletedFalse(String email);

    Optional<User> findByResetTokenAndIsDeletedFalse(String resetToken);

    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findByEmailWithRoles(String email);
}