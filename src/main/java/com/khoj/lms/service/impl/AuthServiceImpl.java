package com.khoj.lms.service.impl;

import com.khoj.lms.dto.auth.*;
import com.khoj.lms.entity.*;
import com.khoj.lms.enums.AuthProvider;
import com.khoj.lms.enums.RoleName;
import com.khoj.lms.exception.*;
import com.khoj.lms.repository.*;
import com.khoj.lms.security.JwtUtil;
import com.khoj.lms.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AuthService handles all authentication business logic.
 *
 * No HTTP concerns here — those belong in AuthController.
 * All DB mutations are @Transactional.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository           userRepository;
    private final RoleRepository           roleRepository;
    private final RefreshTokenRepository   refreshTokenRepository;
    private final UserDetailsService       userDetailsService;
    private final JwtUtil                  jwtUtil;
    private final PasswordEncoder          passwordEncoder;
    private final AuthenticationManager    authenticationManager;

    private static final int    OTP_LENGTH          = 6;
    private static final int    OTP_EXPIRY_MINUTES  = 15;
    private static final int    MAX_OTP_ATTEMPTS    = 5;
    private static final int    MAX_LOGIN_ATTEMPTS  = 5;

    // ─────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────

    /**
     * Registers a new student.
     * Steps:
     *  1. Check email not already taken
     *  2. Hash password
     *  3. Assign STUDENT role
     *  4. Create UserProfile (blank)
     *  5. Generate OTP and "send" (email service — Phase 2)
     */
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        // 1. Duplicate check
        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail().toLowerCase())) {
            throw new DuplicateResourceException(
                    "An account with this email already exists.");
        }

        // 2. Fetch STUDENT role (must exist — seeded in V1 migration)
        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new IllegalStateException(
                        "STUDENT role not found. Check V1 migration."));

        // 3. Build user
        User user = User.builder()
                .firstName(capitalise(request.getFirstName()))
                .lastName(capitalise(request.getLastName()))
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .isEmailVerified(false)
                .build();

        user.addRole(studentRole);

        // 4. Attach blank profile
        UserProfile profile = UserProfile.builder()
                .user(user)
                .build();
        user.setProfile(profile);

        // 5. Generate OTP
        String otp = generateOtp();
        user.setOtpCode(passwordEncoder.encode(otp)); // Store hashed OTP
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        user.setOtpAttempts(0);

        userRepository.save(user);

        log.info("New user registered: {} ({})", user.getFullName(), user.getEmail());

        // TODO Phase 2: emailService.sendOtp(user.getEmail(), otp);
        // For development: log OTP (remove in production)
        log.debug("DEV OTP for {}: {}", user.getEmail(), otp);

        return MessageResponse.builder()
                .success(true)
                .message("Registration successful. Please check your email for the OTP to verify your account.")
                .build();
    }

    // ─────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────

    /**
     * Authenticates a user and returns JWT pair.
     * Steps:
     *  1. Check account exists and not locked
     *  2. Validate credentials via Spring Security
     *  3. Reset failed attempts on success
     *  4. Issue access + refresh tokens
     */
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail().toLowerCase();

        // Load user for pre-auth checks
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        // Check if account is locked
        if (user.getIsLocked()) {
            throw new AuthenticationException(
                    "Your account has been locked. Reason: " + user.getLockedReason());
        }

        // Attempt authentication (throws BadCredentialsException if wrong password)
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (Exception e) {
            // Track failed attempts and auto-lock after MAX_LOGIN_ATTEMPTS
            user.incrementFailedAttempts();
            if (user.getFailedLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
                user.lockAccount("Too many failed login attempts");
                userRepository.save(user);
                throw new AuthenticationException(
                        "Account locked due to too many failed attempts. Please contact support.");
            }
            userRepository.save(user);
            throw new AuthenticationException("Invalid email or password");
        }

        // Success — reset failed attempts, update last login
        user.resetFailedAttempts();
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(getClientIp(httpRequest));
        userRepository.save(user);

        // Generate tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String accessToken = jwtUtil.generateAccessToken(userDetails, user.getId());
        String refreshTokenValue = generateAndSaveRefreshToken(user, httpRequest);

        return buildAuthResponse(user, accessToken, refreshTokenValue);
    }

    // ─────────────────────────────────────────
    // REFRESH TOKEN
    // ─────────────────────────────────────────

    /**
     * Issues new access + refresh tokens (rotation).
     * Old refresh token is marked as used.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String tokenValue = request.getRefreshToken();

        // Validate JWT structure
        if (!jwtUtil.isTokenStructureValid(tokenValue)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        // Look up stored token (DB is source of truth for refresh tokens)
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenAndIsUsedFalseAndIsRevokedFalse(tokenValue)
                .orElseThrow(() -> new AuthenticationException(
                        "Refresh token is invalid, expired, or already used"));

        if (storedToken.isExpired()) {
            throw new AuthenticationException("Refresh token has expired. Please log in again.");
        }

        // Mark old token as used (rotation — each refresh token is one-use)
        storedToken.markUsed();
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();

        // Issue new token pair
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtUtil.generateAccessToken(userDetails, user.getId());
        String newRefreshToken = generateAndSaveRefreshToken(user, httpRequest);

        log.debug("Token refreshed for user: {}", user.getEmail());

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    // ─────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────

    /**
     * Revokes all refresh tokens for the user.
     * Access token expiry is handled by TTL (15 min) — no blacklist needed.
     */
    @Transactional
    public MessageResponse logout(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("User logged out: {}", email);

        return MessageResponse.builder()
                .success(true)
                .message("Logged out successfully.")
                .build();
    }

    // ─────────────────────────────────────────
    // EMAIL VERIFICATION (OTP)
    // ─────────────────────────────────────────

    @Transactional
    public MessageResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (user.getIsEmailVerified()) {
            return MessageResponse.builder().success(true)
                    .message("Email is already verified.").build();
        }

        // OTP expired?
        if (user.getOtpExpiresAt() == null ||
                LocalDateTime.now().isAfter(user.getOtpExpiresAt())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        // Too many attempts?
        if (user.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
            throw new BadRequestException("Too many OTP attempts. Please request a new OTP.");
        }

        // Validate OTP (compare against hashed stored value)
        if (!passwordEncoder.matches(request.getOtpCode(), user.getOtpCode())) {
            user.setOtpAttempts(user.getOtpAttempts() + 1);
            userRepository.save(user);
            throw new BadRequestException("Invalid OTP. "
                    + (MAX_OTP_ATTEMPTS - user.getOtpAttempts()) + " attempts remaining.");
        }

        // Mark verified — clear OTP fields
        user.setIsEmailVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        user.setOtpAttempts(0);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());

        return MessageResponse.builder()
                .success(true)
                .message("Email verified successfully. You can now log in.")
                .build();
    }

    // ─────────────────────────────────────────
    // RESEND OTP
    // ─────────────────────────────────────────

    @Transactional
    public MessageResponse resendOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (user.getIsEmailVerified()) {
            return MessageResponse.builder().success(true)
                    .message("Email is already verified.").build();
        }

        String otp = generateOtp();
        user.setOtpCode(passwordEncoder.encode(otp));
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        user.setOtpAttempts(0);
        userRepository.save(user);

        // TODO Phase 2: emailService.sendOtp(user.getEmail(), otp);
        log.debug("DEV Resent OTP for {}: {}", user.getEmail(), otp);

        return MessageResponse.builder()
                .success(true)
                .message("A new OTP has been sent to your email.")
                .build();
    }

    // ─────────────────────────────────────────
    // FORGOT PASSWORD
    // ─────────────────────────────────────────

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        // Always return success — never reveal if email exists (security best practice)
        userRepository.findByEmailAndIsDeletedFalse(request.getEmail().toLowerCase())
                .ifPresent(user -> {
                    String resetToken = UUID.randomUUID().toString().replace("-", "");
                    user.setResetToken(resetToken);
                    user.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
                    userRepository.save(user);
                    // TODO Phase 2: emailService.sendPasswordReset(user.getEmail(), resetToken);
                    log.debug("DEV Reset token for {}: {}", user.getEmail(), resetToken);
                });

        return MessageResponse.builder()
                .success(true)
                .message("If an account with this email exists, a password reset link has been sent.")
                .build();
    }

    // ─────────────────────────────────────────
    // RESET PASSWORD
    // ─────────────────────────────────────────

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetTokenAndIsDeletedFalse(request.getResetToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token."));

        if (user.getResetTokenExpiresAt() == null ||
                LocalDateTime.now().isAfter(user.getResetTokenExpiresAt())) {
            throw new BadRequestException("Reset token has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        user.resetFailedAttempts();
        user.unlockAccount();
        userRepository.save(user);

        // Revoke all existing refresh tokens for security
        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("Password reset for user: {}", user.getEmail());

        return MessageResponse.builder()
                .success(true)
                .message("Password reset successfully. Please log in with your new password.")
                .build();
    }

    // ─────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────

    private String generateAndSaveRefreshToken(User user, HttpServletRequest request) {
        String tokenValue = jwtUtil.generateRefreshToken(user.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .expiresAt(LocalDateTime.now().plusSeconds(
                        jwtUtil.getRefreshTokenExpiryMs() / 1000))
                .userAgent(request.getHeader("User-Agent"))
                .ipAddress(getClientIp(request))
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        Set<RoleName> roleNames = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)  // 15 minutes in seconds
                .user(AuthResponse.UserSummary.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .profilePictureUrl(user.getProfilePictureUrl())
                        .roles(roleNames)
                        .emailVerified(user.getIsEmailVerified())
                        .build())
                .build();
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }

    private String capitalise(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Transactional(readOnly = true)
    public AuthResponse.UserSummary getCurrentUser(String email) {
        User user = userRepository.findWithRolesByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Set<RoleName> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return AuthResponse.UserSummary.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .profilePictureUrl(user.getProfilePictureUrl())
                .roles(roleNames)
                .emailVerified(user.getIsEmailVerified())
                .build();
    }
}