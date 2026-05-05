package com.khoj.lms.service;

import com.khoj.lms.dto.auth.*;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    MessageResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);

    AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest);

    MessageResponse logout(String email);

    MessageResponse verifyEmail(VerifyEmailRequest request);

    MessageResponse resendOtp(ResendOtpRequest request);

    MessageResponse forgotPassword(ForgotPasswordRequest request);

    MessageResponse resetPassword(ResetPasswordRequest request);

    AuthResponse.UserSummary getCurrentUser(String email);
}