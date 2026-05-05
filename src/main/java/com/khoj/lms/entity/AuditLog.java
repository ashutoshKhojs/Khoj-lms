package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Immutable audit trail for critical actions.
 * Written once, never updated — no soft delete.
 * Examples: login, password reset, role change, account lock.
 *
 * Table: audit_logs
 */
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_user_id", columnList = "user_id"),
                @Index(name = "idx_audit_action", columnList = "action"),
                @Index(name = "idx_audit_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {

    @Column(name = "user_id")
    private UUID userId;            // Nullable — some actions are system-initiated

    @Column(name = "action", nullable = false, length = 100)
    private String action;          // e.g. "USER_LOGIN", "PASSWORD_RESET", "ROLE_ASSIGNED"

    @Column(name = "entity_type", length = 50)
    private String entityType;      // e.g. "User", "Course"

    @Column(name = "entity_id", length = 50)
    private String entityId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "status", length = 20)
    private String status;          // "SUCCESS" | "FAILURE"

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;        // JSON for extra context (old value → new value etc.)
}