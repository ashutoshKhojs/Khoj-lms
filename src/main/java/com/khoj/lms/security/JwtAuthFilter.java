package com.khoj.lms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khoj.lms.audit.AuditLogger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil                jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuditLogger            auditLogger;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         filterChain)
            throws ServletException, IOException {

        String uri   = request.getRequestURI();
        String token = extractTokenFromRequest(request);

        // No token — pass through
        // Spring Security will return 401 for protected routes
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Already authenticated in this request
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Fast structure check before hitting DB
            if (!jwtUtil.isTokenStructureValid(token)) {
                log.warn("Malformed JWT structure — uri={} ip={}",
                        uri, getClientIp(request));

                auditLogger.suspiciousActivity(
                        "unknown",
                        getClientIp(request),
                        "MALFORMED_JWT: bad token structure on " + uri
                );

                sendErrorResponse(response,
                        HttpStatus.UNAUTHORIZED,
                        "Invalid token format.");
                return;
            }

            // Reject refresh tokens used as access tokens
            String tokenType = jwtUtil.extractTokenType(token);
            if (!"access".equals(tokenType)) {
                log.warn("Refresh token used as access token — uri={} ip={}",
                        uri, getClientIp(request));

                auditLogger.suspiciousActivity(
                        "unknown",
                        getClientIp(request),
                        "WRONG_TOKEN_TYPE: refresh token used as access on " + uri
                );

                sendErrorResponse(response,
                        HttpStatus.UNAUTHORIZED,
                        "Invalid token type. Please login again.");
                return;
            }

            String email = jwtUtil.extractEmail(token);

            if (StringUtils.hasText(email)) {
                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                if (jwtUtil.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request));

                    SecurityContextHolder.getContext()
                            .setAuthentication(authToken);

                    log.debug("JWT authenticated — user={} uri={}",
                            email, uri);

                } else {
                    // Token expired or invalid
                    log.warn("JWT token invalid or expired — user={} uri={} ip={}",
                            email, uri, getClientIp(request));

                    sendErrorResponse(response,
                            HttpStatus.UNAUTHORIZED,
                            "Token has expired. Please login again.");
                    return;
                }
            }

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // Token structurally valid but expired
            log.warn("JWT expired — uri={} ip={} reason={}",
                    uri, getClientIp(request), e.getMessage());

            sendErrorResponse(response,
                    HttpStatus.UNAUTHORIZED,
                    "Token has expired. Please login again.");
            return;

        } catch (io.jsonwebtoken.MalformedJwtException |
                 io.jsonwebtoken.security.SignatureException e) {
            // Possible token tampering
            log.warn("JWT signature invalid — uri={} ip={} reason={}",
                    uri, getClientIp(request), e.getMessage());

            auditLogger.suspiciousActivity(
                    "unknown",
                    getClientIp(request),
                    "INVALID_JWT_SIGNATURE: possible tampering on " + uri
            );

            sendErrorResponse(response,
                    HttpStatus.UNAUTHORIZED,
                    "Invalid token.");
            return;

        } catch (Exception e) {
            // Unexpected — logs to main + error file
            log.error("Unexpected JWT filter error — uri={} type={} message={}",
                    uri, e.getClass().getSimpleName(), e.getMessage(), e);

            sendErrorResponse(response,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Authentication error. Please try again.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken)
                && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void sendErrorResponse(HttpServletResponse response,
                                   HttpStatus status,
                                   String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "success",   false,
                "message",   message,
                "timestamp", java.time.LocalDateTime.now().toString()
        );

        new ObjectMapper().writeValue(response.getWriter(), body);
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP"
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
}