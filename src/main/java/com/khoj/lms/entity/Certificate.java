package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import com.khoj.lms.enums.CertificateStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Certificate issued to a student upon course completion.
 *
 * Uniquely identified by certificateNumber (e.g. KHOJ-2024-00001).
 * PDF stored on S3. Public verification URL allows employers to verify.
 *
 * Table: certificates
 */
@Entity
@Table(
        name = "certificates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_certificate_student_course",
                        columnNames = {"student_id", "course_id"}
                )
        },
        indexes = {
                @Index(name = "idx_cert_student",     columnList = "student_id"),
                @Index(name = "idx_cert_course",      columnList = "course_id"),
                @Index(name = "idx_cert_number",      columnList = "certificate_number"),
                @Index(name = "idx_cert_verify_code", columnList = "verification_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    // ─────────────────────────────────────────
    // Certificate Identity
    // ─────────────────────────────────────────

    /** Human-readable unique number — e.g. "KHOJ-2024-00001" */
    @Column(name = "certificate_number", nullable = false, unique = true, length = 50)
    private String certificateNumber;

    /**
     * Short random code for public verification.
     * URL: /verify/{verificationCode}
     * Lets employers verify authenticity without student login.
     */
    @Column(name = "verification_code", nullable = false, unique = true, length = 20)
    private String verificationCode;

    // ─────────────────────────────────────────
    // Status & File
    // ─────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CertificateStatus status = CertificateStatus.PENDING;

    /** S3 key for the generated PDF */
    @Column(name = "pdf_s3_key", length = 500)
    private String pdfS3Key;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    // ─────────────────────────────────────────
    // Snapshot of data at time of issue
    // (course name/instructor may change later)
    // ─────────────────────────────────────────

    @Column(name = "student_name_snapshot", nullable = false, length = 150)
    private String studentNameSnapshot;

    @Column(name = "course_title_snapshot", nullable = false, length = 200)
    private String courseTitleSnapshot;

    @Column(name = "instructor_name_snapshot", length = 150)
    private String instructorNameSnapshot;

    @Column(name = "completion_percentage_snapshot")
    private Double completionPercentageSnapshot;

    // ─────────────────────────────────────────
    // Revocation
    // ─────────────────────────────────────────

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason", length = 500)
    private String revokedReason;

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────

    public void issue(String pdfS3Key) {
        this.pdfS3Key = pdfS3Key;
        this.status = CertificateStatus.ISSUED;
        this.issuedAt = LocalDateTime.now();
    }

    public void revoke(String reason) {
        this.status = CertificateStatus.REVOKED;
        this.revokedAt = LocalDateTime.now();
        this.revokedReason = reason;
    }

    public boolean isValid() {
        return CertificateStatus.ISSUED.equals(this.status);
    }
}