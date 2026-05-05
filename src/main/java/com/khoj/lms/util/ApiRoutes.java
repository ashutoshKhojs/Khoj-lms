package com.khoj.lms.util;

public final class ApiRoutes {

    private ApiRoutes() {} // prevent instantiation

    public static final String BASE = "/api";

    public static final class Auth {
        public static final String BASE = "/auth";

        public static final String REGISTER = "/register";
        public static final String LOGIN = "/login";
        public static final String REFRESH = "/refresh";
        public static final String LOGOUT = "/logout";

        public static final String VERIFY_EMAIL = "/verify-email";
        public static final String RESEND_OTP = "/resend-otp";

        public static final String FORGOT_PASSWORD = "/forgot-password";
        public static final String RESET_PASSWORD = "/reset-password";

        public static final String ME = "/me";
    }
}