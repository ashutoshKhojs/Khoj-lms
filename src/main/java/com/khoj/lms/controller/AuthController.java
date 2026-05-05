package com.khoj.lms.controller;

import com.khoj.lms.dto.auth.*;
import com.khoj.lms.dto.common.ApiResponse;
import com.khoj.lms.service.AuthService;
import com.khoj.lms.util.ApiRoutes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiRoutes.BASE + ApiRoutes.Auth.BASE)
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh, email verification")
public class AuthController {

    private final AuthService authService;

    // ─────────────────────────────────────────
    // Register
    // ─────────────────────────────────────────

    @PostMapping(ApiRoutes.Auth.REGISTER)
    @Operation(summary = "Register a new student account")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request) {

        MessageResponse res = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(res.getMessage()));
    }

    // ─────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────

    @PostMapping(ApiRoutes.Auth.LOGIN)
    @Operation(summary = "Login and receive JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse response = authService.login(request, httpRequest);

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", response)
        );
    }

    // ─────────────────────────────────────────
    // Refresh Token
    // ─────────────────────────────────────────

    @PostMapping(ApiRoutes.Auth.REFRESH)
    @Operation(summary = "Issue new access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse response = authService.refreshToken(request, httpRequest);

        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed successfully", response)
        );
    }

    // ─────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────

    @PostMapping(ApiRoutes.Auth.LOGOUT)
    @Operation(summary = "Revoke refresh tokens and log out")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails userDetails) {

        MessageResponse res = authService.logout(userDetails.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success(res.getMessage())
        );
    }

    // ─────────────────────────────────────────
    // Email Verification
    // ─────────────────────────────────────────

    @PostMapping(ApiRoutes.Auth.VERIFY_EMAIL)
    @Operation(summary = "Verify email address using OTP")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {

        MessageResponse res = authService.verifyEmail(request);

        return ResponseEntity.ok(
                ApiResponse.success(res.getMessage())
        );
    }

    @PostMapping(ApiRoutes.Auth.RESEND_OTP)
    @Operation(summary = "Resend OTP verification code")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {

        MessageResponse res = authService.resendOtp(request);

        return ResponseEntity.ok(
                ApiResponse.success(res.getMessage())
        );
    }

    // ─────────────────────────────────────────
    // Password Reset
    // ─────────────────────────────────────────

    @PostMapping(ApiRoutes.Auth.FORGOT_PASSWORD)
    @Operation(summary = "Request a password reset link")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        MessageResponse res = authService.forgotPassword(request);

        return ResponseEntity.ok(
                ApiResponse.success(res.getMessage())
        );
    }

    @PostMapping(ApiRoutes.Auth.RESET_PASSWORD)
    @Operation(summary = "Reset password using reset token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        MessageResponse res = authService.resetPassword(request);

        return ResponseEntity.ok(
                ApiResponse.success(res.getMessage())
        );
    }

    // ─────────────────────────────────────────
    // Current User
    // ─────────────────────────────────────────

    @GetMapping(ApiRoutes.Auth.ME)
    @Operation(summary = "Get current authenticated user info")
    public ResponseEntity<ApiResponse<AuthResponse.UserSummary>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        AuthResponse.UserSummary user =
                authService.getCurrentUser(userDetails.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success("User fetched successfully", user)
        );
    }
}