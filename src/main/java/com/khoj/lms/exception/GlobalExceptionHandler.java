package com.khoj.lms.exception;

import com.khoj.lms.audit.AuditLogger;
import com.khoj.lms.dto.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final AuditLogger auditLogger;

    // ─────────────────────────────────────────
    // Validation Errors — @Valid failures
    // LOGS TO: main log only (client mistake)
    // ─────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Validation failed — fields={} errors={}",
                errors.keySet(), errors.values());
        // ✗ NOT audit — just a form validation error

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", errors));
    }

    // ─────────────────────────────────────────
    // Missing Request Param
    // LOGS TO: main log only
    // ─────────────────────────────────────────

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex) {

        log.warn("Missing request parameter — param='{}' type='{}'",
                ex.getParameterName(), ex.getParameterType());
        // ✗ NOT audit — client forgot a param

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        "Missing required parameter: " + ex.getParameterName()));
    }

    // ─────────────────────────────────────────
    // Wrong Type in Request Param / Path Var
    // LOGS TO: main log only
    // ─────────────────────────────────────────

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        log.warn("Type mismatch — param='{}' value='{}' expectedType='{}'",
                ex.getName(), ex.getValue(),
                ex.getRequiredType() != null
                        ? ex.getRequiredType().getSimpleName() : "unknown");
        // ✗ NOT audit — wrong type in URL param

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        "Invalid value '" + ex.getValue() +
                                "' for parameter '" + ex.getName() + "'"));
    }

    // ─────────────────────────────────────────
    // Resource Not Found — 404
    // LOGS TO: main log only
    // ─────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            ResourceNotFoundException ex) {

        log.warn("Resource not found — {}", ex.getMessage());
        // ✗ NOT audit — routine not found

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ─────────────────────────────────────────
    // Duplicate Resource — 409
    // LOGS TO: main log only
    // ─────────────────────────────────────────

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(
            DuplicateResourceException ex) {

        log.warn("Duplicate resource — {}", ex.getMessage());
        // ✗ NOT audit — e.g. email already exists

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ─────────────────────────────────────────
    // Bad Request — 400
    // LOGS TO: main log only
    // ─────────────────────────────────────────

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            BadRequestException ex) {

        log.warn("Bad request — {}", ex.getMessage());
        // ✗ NOT audit — general bad input

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ─────────────────────────────────────────
    // Spring Security — Bad Credentials (most specific first)
    // LOGS TO: main log + AUDIT LOG ✅
    // Wrong password attempt
    // ─────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex) {

        log.warn("Bad credentials attempt — {}", ex.getMessage());

        // ✅ AUDIT — failed login attempt is a security event
        auditLogger.loginFailed("unknown", "unknown", 0);

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid email or password"));
    }

    // ─────────────────────────────────────────
    // Spring Security — Account Locked
    // LOGS TO: main log + AUDIT LOG ✅
    // ─────────────────────────────────────────

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(
            LockedException ex) {

        log.warn("Locked account access attempt — {}", ex.getMessage());

        auditLogger.suspiciousActivity(
                "unknown",
                "unknown",
                "LOCKED_ACCOUNT_ACCESS: " + ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        "Your account has been locked. Please contact support."));
    }

    // ─────────────────────────────────────────
    // Spring Security — Account Disabled
    // LOGS TO: main log + AUDIT LOG ✅
    // ─────────────────────────────────────────

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(
            DisabledException ex) {

        log.warn("Disabled account access attempt — {}", ex.getMessage());

        auditLogger.suspiciousActivity(
                "unknown",
                "unknown",
                "DISABLED_ACCOUNT_ACCESS: " + ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        "Your account has been disabled. Please contact support."));
    }

    // ─────────────────────────────────────────
    // Custom AuthenticationException — 401
    // LOGS TO: main log + AUDIT LOG ✅
    // (Generic fallback for non-Spring auth issues)
    // ─────────────────────────────────────────

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(
            AuthenticationException ex) {

        log.warn("Authentication failed — {}", ex.getMessage());

        auditLogger.loginFailed("unknown", "unknown", 0);

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ─────────────────────────────────────────
    // Custom Access Denied — 403
    // LOGS TO: main log + AUDIT LOG ✅
    // ─────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex) {

        log.warn("Access denied — {}", ex.getMessage());

        auditLogger.suspiciousActivity(
                "unknown",
                "unknown",
                "ACCESS_DENIED: " + ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ─────────────────────────────────────────
    // Spring Security — Access Denied
    // LOGS TO: main log + AUDIT LOG ✅
    // @PreAuthorize failure — wrong role trying restricted endpoint
    // ─────────────────────────────────────────

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSpringAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {

        log.warn("Spring Security access denied — {}", ex.getMessage());

        auditLogger.suspiciousActivity(
                "unknown",
                "unknown",
                "ROLE_VIOLATION: " + ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        "You don't have permission to perform this action."));
    }

    // ─────────────────────────────────────────
    // Mail Exception — SMTP failures (defensive)
    // LOGS TO: error log — NEVER bubble SMTP details to client
    //
    // Emails are normally @Async (background thread, never reach this handler).
    // This is a safety net in case someone calls a mail method synchronously
    // in the future, OR if @Async submission itself fails inside a request.
    // ─────────────────────────────────────────

    @ExceptionHandler(MailException.class)
    public ResponseEntity<ApiResponse<Void>> handleMail(MailException ex) {

        log.error("Mail send failure — type={} message={}",
                ex.getClass().getSimpleName(), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "Email delivery failed. Your action was completed " +
                                "but the notification email could not be sent."));
    }

    // ─────────────────────────────────────────
    // Illegal State — internal config issues
    // LOGS TO: main log + error log (ERROR level)
    // ─────────────────────────────────────────

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(
            IllegalStateException ex) {

        log.error("Illegal state — {}", ex.getMessage(), ex);
        // ✗ NOT audit — server config issue, not a user action

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "Internal configuration error. Please contact support."));
    }

    // ─────────────────────────────────────────
    // Fallback — catch everything else
    // LOGS TO: main log + error log (ERROR level)
    // ─────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(Exception ex) {

        log.error("Unexpected error — type={} message={}",
                ex.getClass().getSimpleName(), ex.getMessage(), ex);
        // ✗ NOT audit — unexpected crash, not a user security action

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "Something went wrong. Please try again later."));
    }
}