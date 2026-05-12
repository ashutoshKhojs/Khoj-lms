package com.khoj.lms.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Dedicated audit logger.
 * Writes ONLY to logs/current/khoj-lms-audit.log
 *
 * Use for:
 *   - Auth events (login, logout, register)
 *   - Security events (lock, failed attempts)
 *   - Role changes
 *   - Admin actions (approve/reject instructor, course)
 *   - Password reset
 *   - File uploads
 */
@Component
public class AuditLogger {

    // Must match logger name in logback-spring.xml exactly
    private static final Logger audit =
            LoggerFactory.getLogger("com.khoj.lms.audit");

    // ── Auth Events ───────────────────────────────────

    public void userRegistered(String email) {
        audit.info("event=USER_REGISTERED email={}", email);
    }

    public void emailVerified(String email) {
        audit.info("event=EMAIL_VERIFIED email={}", email);
    }

    public void otpResent(String email) {
        audit.info("event=OTP_RESENT email={}", email);
    }

    public void userLoggedIn(String email, String ip) {
        audit.info("event=USER_LOGIN email={} ip={}", email, ip);
    }

    public void userLoggedOut(String email) {
        audit.info("event=USER_LOGOUT email={}", email);
    }

    public void tokenRefreshed(String email) {
        audit.info("event=TOKEN_REFRESHED email={}", email);
    }

    // ── Security Events ───────────────────────────────

    public void loginFailed(String email, String ip, int attempts) {
        audit.warn("event=LOGIN_FAILED email={} ip={} attempts={}",
                email, ip, attempts);
    }

    public void accountLocked(String email, String reason) {
        audit.warn("event=ACCOUNT_LOCKED email={} reason={}",
                email, reason);
    }

    public void accountUnlocked(String email, String unlockedBy) {
        audit.info("event=ACCOUNT_UNLOCKED email={} unlockedBy={}",
                email, unlockedBy);
    }

    public void suspiciousActivity(String email, String ip, String detail) {
        audit.warn("event=SUSPICIOUS_ACTIVITY email={} ip={} detail={}",
                email, ip, detail);
    }

    // ── Password Events ───────────────────────────────

    public void passwordResetRequested(String email) {
        audit.info("event=PASSWORD_RESET_REQUESTED email={}", email);
    }

    public void passwordResetCompleted(String email) {
        audit.info("event=PASSWORD_RESET_COMPLETED email={}", email);
    }

    // ── Role Events ───────────────────────────────────

    public void roleGranted(String email, String role, String grantedBy) {
        audit.info("event=ROLE_GRANTED email={} role={} grantedBy={}",
                email, role, grantedBy);
    }

    public void roleRevoked(String email, String role, String revokedBy) {
        audit.warn("event=ROLE_REVOKED email={} role={} revokedBy={}",
                email, role, revokedBy);
    }

    // ── Instructor Application Events ─────────────────

    public void instructorApplied(String email) {
        audit.info("event=INSTRUCTOR_APPLIED email={}", email);
    }

    public void instructorApproved(String email, String approvedBy) {
        audit.info("event=INSTRUCTOR_APPROVED email={} approvedBy={}",
                email, approvedBy);
    }

    public void instructorRejected(String email,
                                   String rejectedBy,
                                   String reason) {
        audit.warn("event=INSTRUCTOR_REJECTED email={} rejectedBy={} reason={}",
                email, rejectedBy, reason);
    }

    // ── Course Events ─────────────────────────────────

    public void courseCreated(String courseTitle, String instructorEmail) {
        audit.info("event=COURSE_CREATED title='{}' instructor={}",
                courseTitle, instructorEmail);
    }

    public void courseSubmitted(String courseTitle, String instructorEmail) {
        audit.info("event=COURSE_SUBMITTED title='{}' instructor={}",
                courseTitle, instructorEmail);
    }

    public void courseApproved(String courseTitle, String approvedBy) {
        audit.info("event=COURSE_APPROVED title='{}' approvedBy={}",
                courseTitle, approvedBy);
    }

    public void courseRejected(String courseTitle,
                               String rejectedBy,
                               String reason) {
        audit.warn("event=COURSE_REJECTED title='{}' rejectedBy={} reason={}",
                courseTitle, rejectedBy, reason);
    }

    public void courseDeleted(String courseTitle, String deletedBy) {
        audit.warn("event=COURSE_DELETED title='{}' deletedBy={}",
                courseTitle, deletedBy);
    }

    // ── Upload Events ─────────────────────────────────

    public void videoUploadInitiated(String email, String s3Key) {
        audit.info("event=VIDEO_UPLOAD_INITIATED email={} s3Key={}",
                email, s3Key);
    }

    public void videoUploadCompleted(String email, String s3Key) {
        audit.info("event=VIDEO_UPLOAD_COMPLETED email={} s3Key={}",
                email, s3Key);
    }

    public void videoUploadAborted(String email, String s3Key) {
        audit.warn("event=VIDEO_UPLOAD_ABORTED email={} s3Key={}",
                email, s3Key);
    }

    public void imageUploaded(String email, String s3Key) {
        audit.info("event=IMAGE_UPLOADED email={} s3Key={}",
                email, s3Key);
    }

    // ── Admin Events ──────────────────────────────────

    public void adminAction(String adminEmail,
                            String action,
                            String targetEmail) {
        audit.info("event=ADMIN_ACTION admin={} action={} target={}",
                adminEmail, action, targetEmail);
    }

    public void userDeletedByAdmin(String targetEmail, String adminEmail) {
        audit.warn("event=USER_DELETED_BY_ADMIN target={} admin={}",
                targetEmail, adminEmail);
    }
}