package com.khoj.lms.util;

public final class ApiRoutes {

    private ApiRoutes() {}

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

    public static final class Instructor {

        public static final String BASE = "/instructor-applications";

        public static final String APPLY = "/apply";
        public static final String MY_APPLICATION = "/my-application";

        // Admin
        public static final String ADMIN_BASE = "/admin/instructor-applications";
        public static final String LIST = "/list";
        public static final String APPROVE = "/{id}/approve";
        public static final String REJECT = "/{id}/reject";
    }

    // ✅ ADD THIS
    public static final class Lesson {

        // PUBLIC
        public static final String BASE = "/lessons";

        public static final String GET_BY_MODULE = "/module/{moduleId}";
        public static final String GET_BY_ID = "/{id}";

        // INSTRUCTOR
        public static final String INSTRUCTOR_BASE = "/instructor/lessons";

        public static final String CREATE = "/module/{moduleId}";
        public static final String UPDATE = "/{id}";
        public static final String TOGGLE_PUBLISH = "/{id}/toggle-publish";
        public static final String DELETE = "/{id}";
        public static final String REORDER = "/reorder";
    }


    public static final class Course {

        // PUBLIC
        public static final String BASE = "/courses";

        public static final String LIST = "";
        public static final String GET_BY_SLUG = "/{slug}";

        // INSTRUCTOR
        public static final String INSTRUCTOR_BASE = "/instructor/courses";

        public static final String INSTRUCTOR_LIST = "";
        public static final String CREATE = "";
        public static final String UPDATE = "/{id}";
        public static final String SUBMIT = "/{id}/submit";
        public static final String DELETE = "/{id}";

        // ADMIN
        public static final String ADMIN_BASE = "/admin/courses";

        public static final String ADMIN_LIST = "";
        public static final String APPROVE = "/{id}/approve";
        public static final String REJECT = "/{id}/reject";
        public static final String ARCHIVE = "/{id}/archive";
    }


    public static final class Module {

        // PUBLIC
        public static final String BASE = "/modules";

        public static final String GET_BY_COURSE = "/course/{courseId}";
        public static final String GET_BY_ID = "/{id}";

        // INSTRUCTOR
        public static final String INSTRUCTOR_BASE = "/instructor/modules";

        public static final String CREATE = "/course/{courseId}";
        public static final String UPDATE = "/{id}";
        public static final String TOGGLE_PUBLISH = "/{id}/toggle-publish";
        public static final String DELETE = "/{id}";
        public static final String REORDER = "/reorder";
    }

    public static final class Category {

        public static final String BASE = "/categories";

        // PUBLIC
        public static final String LIST = "";
        public static final String GET_BY_ID = "/{id}";

        // ADMIN
        public static final String ADMIN_BASE = "/admin/categories";
        public static final String CREATE = "";
        public static final String UPDATE = "/{id}";
        public static final String DELETE = "/{id}";
    }

    public static final class Upload {

        public static final String BASE = "/upload";

        // VIDEO
        public static final String VIDEO_PRESIGN = "/video/presign";
        public static final String COMPLETE_MULTIPART = "/video/complete-multipart";
        public static final String ABORT_MULTIPART = "/video/abort-multipart";

        // IMAGE
        public static final String IMAGE_PRESIGN = "/image/presign";

        // STREAM
        public static final String STREAM_URL = "/stream-url";
    }


}