package com.khoj.lms.enums;

/**
 * Lifecycle of a certificate.
 *
 * PENDING  → Course completed, certificate generation in queue
 * ISSUED   → PDF generated and available for download
 * REVOKED  → Admin revoked (e.g. academic dishonesty)
 */
public enum CertificateStatus {
    PENDING,
    ISSUED,
    REVOKED
}