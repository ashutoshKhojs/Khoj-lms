package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import com.khoj.lms.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a system role (STUDENT, INSTRUCTOR, ADMIN).
 * Roles are pre-seeded via data.sql — never created at runtime.
 *
 * Table: roles
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 20)
    private RoleName name;

    @Column(name = "description", length = 255)
    private String description;

    // Bidirectional — mapped by "roles" in User entity
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();

    // Convenience constructor for seeding
    public Role(RoleName name, String description) {
        this.name = name;
        this.description = description;
    }
}