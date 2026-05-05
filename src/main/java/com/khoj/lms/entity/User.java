package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import com.khoj.lms.enums.AuthProvider;
import com.khoj.lms.enums.Gender;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Core user entity for the Khoj LMS platform.
 *
 * Supports:
 *  - Email + password login (Phase 1)
 *  - Google OAuth (Phase 2 — provider fields ready)
 *  - Soft delete (inherited from BaseEntity)
 *  - Multi-role assignment (Student can also be Instructor)
 *  - OTP-based email verification
 *
 * Table: users
 */
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_is_deleted", columnList = "is_deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    // ─────────────────────────────────────────
    // Basic Identity
    // ─────────────────────────────────────────

    @NotBlank
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /**
     * Hashed password (BCrypt).
     * Nullable for OAuth users (Google login won't have a password).
     */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    // ─────────────────────────────────────────
    // Authentication & Provider
    // ─────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    /**
     * Provider's unique ID — used when auth_provider = GOOGLE.
     * Stored to prevent duplicate account creation on re-login.
     */
    @Column(name = "provider_id", length = 255)
    private String providerId;

    // ─────────────────────────────────────────
    // Account Status
    // ─────────────────────────────────────────

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_email_verified", nullable = false)
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "is_phone_verified", nullable = false)
    @Builder.Default
    private Boolean isPhoneVerified = false;

    /** Locked by admin — prevents login without deleting the account */
    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "locked_reason", length = 255)
    private String lockedReason;

    // ─────────────────────────────────────────
    // OTP Verification (Phase 1 — email OTP)
    // ─────────────────────────────────────────

    @Column(name = "otp_code", length = 10)
    private String otpCode;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    @Column(name = "otp_attempts", nullable = false)
    @Builder.Default
    private Integer otpAttempts = 0;

    // ─────────────────────────────────────────
    // Password Reset
    // ─────────────────────────────────────────

    @Column(name = "reset_token", length = 255)
    private String resetToken;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    // ─────────────────────────────────────────
    // Login Tracking
    // ─────────────────────────────────────────

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    // ─────────────────────────────────────────
    // Roles (Many-to-Many)
    // ─────────────────────────────────────────

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // ─────────────────────────────────────────
    // Profile (One-to-One)
    // ─────────────────────────────────────────

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private UserProfile profile;

    // ─────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void addRole(Role role) {
        this.roles.add(role);
        role.getUsers().add(this);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
        role.getUsers().remove(this);
    }

    public boolean hasRole(String roleName) {
        return this.roles.stream()
                .anyMatch(r -> r.getName().name().equalsIgnoreCase(roleName));
    }

    public void lockAccount(String reason) {
        this.isLocked = true;
        this.lockedAt = LocalDateTime.now();
        this.lockedReason = reason;
    }

    public void unlockAccount() {
        this.isLocked = false;
        this.lockedAt = null;
        this.lockedReason = null;
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
    }
}