package com.khoj.lms.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

/**
 * Runs on every HTTP request.
 * Populates MDC with requestId, userEmail, clientIp.
 * These appear in EVERY log line for that request automatically.
 * Also logs request start and end with duration.
 */
@Component
@Order(1)
@Slf4j
public class RequestLoggingFilter implements Filter {

    // Skip logging for these — too noisy, not useful
    private static final String[] SKIP_URLS = {
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui",
            "/api-docs",
            "/favicon.ico"
    };

    @Override
    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri = request.getRequestURI();

        // Skip health checks and swagger from logging
        if (shouldSkip(uri)) {
            chain.doFilter(req, res);
            return;
        }

        // Generate unique request ID — 8 char uppercase
        String requestId = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        String clientIp  = extractClientIp(request);
        String userEmail = extractUserEmail(request);
        String method    = request.getMethod();

        // Populate MDC — flows into every log line automatically
        MDC.put("requestId", requestId);
        MDC.put("clientIp",  clientIp);
        MDC.put("userEmail", userEmail);

        // Add to response header — client can use for support tickets
        response.setHeader("X-Request-ID", requestId);

        long startTime = System.currentTimeMillis();

        try {
            log.info("→ {} {} [ip={}] [user={}]",
                    method, uri, clientIp, userEmail);

            chain.doFilter(req, res);

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            int status = response.getStatus();

            // Log differently based on status code
            if (status >= 500) {
                log.error("← {} {} {} {}ms",
                        method, uri, status, duration);
            } else if (status >= 400) {
                log.warn("← {} {} {} {}ms",
                        method, uri, status, duration);
            } else {
                log.info("← {} {} {} {}ms",
                        method, uri, status, duration);
            }

            // CRITICAL — always clear MDC after request
            // Prevents MDC leaking to next request on same thread
            MDC.clear();
        }
    }

    private boolean shouldSkip(String uri) {
        for (String skip : SKIP_URLS) {
            if (uri.startsWith(skip)) return true;
        }
        return false;
    }

    private String extractClientIp(HttpServletRequest request) {
        // Check proxy headers first
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };
        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()
                    && !"unknown".equalsIgnoreCase(value)) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String extractUserEmail(HttpServletRequest request) {
        // Extract from JWT without full verification — for logging only
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return "anonymous";
        }
        try {
            String[] parts  = auth.split("\\.");
            if (parts.length < 2) return "anonymous";
            String payload  = new String(
                    Base64.getUrlDecoder().decode(parts[1]));
            if (payload.contains("\"sub\":\"")) {
                return payload.split("\"sub\":\"")[1].split("\"")[0];
            }
        } catch (Exception ignored) {}
        return "anonymous";
    }
}