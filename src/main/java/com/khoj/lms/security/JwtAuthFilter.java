package com.khoj.lms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter.
 *
 * Runs once per request BEFORE UsernamePasswordAuthenticationFilter.
 *
 * Flow:
 *  1. Read Authorization: Bearer <token> header
 *  2. Validate token structure + signature
 *  3. Extract email from token
 *  4. Load UserDetails from DB
 *  5. Validate token against UserDetails (expiry + email match)
 *  6. Set authentication in SecurityContext → request proceeds as authenticated
 *
 * If anything fails, SecurityContext stays empty → Spring Security returns 401.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String token = extractTokenFromRequest(request);

        // No token — let Spring Security handle it (will return 401 for protected routes)
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Already authenticated in this request (shouldn't happen but guard anyway)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Fast structure check before hitting the DB
            if (!jwtUtil.isTokenStructureValid(token)) {
                log.debug("Invalid JWT structure for request: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // Reject refresh tokens being used as access tokens
            String tokenType = jwtUtil.extractTokenType(token);
            if (!"access".equals(tokenType)) {
                log.debug("Refresh token used as access token, rejecting");
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtUtil.extractEmail(token);

            if (StringUtils.hasText(email)) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtUtil.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authenticated user '{}' for request: {}", email, request.getRequestURI());
                }
            }
        } catch (Exception e) {
            log.error("Could not set user authentication for {}: {}",
                    request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}