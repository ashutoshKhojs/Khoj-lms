package com.khoj.lms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khoj.lms.audit.AuditLogger;
import com.khoj.lms.security.JwtAuthFilter;
import com.khoj.lms.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthFilter          jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuditLogger            auditLogger;

    private static final String[] PUBLIC_ROUTES = {
            "/auth/**",
            "/courses",
            "/courses/{slug}",
            "/categories/**",
            "/certificates/verify/**",
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/coupons/public",
            "/coupons/applicable/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET,  PUBLIC_ROUTES).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/lessons/*/preview").permitAll()
                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex

                        // ── 401 — No token / invalid token ──────────
                        .authenticationEntryPoint((request, response, authException) -> {

                            log.warn("Unauthorized — uri={} ip={} reason={}",
                                    request.getRequestURI(),
                                    request.getRemoteAddr(),
                                    authException.getMessage());

                            auditLogger.suspiciousActivity(
                                    "unknown",
                                    request.getRemoteAddr(),
                                    "UNAUTHORIZED_ACCESS: " + request.getRequestURI()
                            );

                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");

                            Map<String, Object> body = Map.of(
                                    "success", false,
                                    "message", "Authentication required. Please login.",
                                    "timestamp", java.time.LocalDateTime.now().toString()
                            );

                            new ObjectMapper()
                                    .writeValue(response.getWriter(), body);
                        })

                        // ── 403 — Wrong role / insufficient permissions ──
                        .accessDeniedHandler((request, response, accessDeniedException) -> {

                            log.warn("Forbidden — uri={} ip={} reason={}",
                                    request.getRequestURI(),
                                    request.getRemoteAddr(),
                                    accessDeniedException.getMessage());

                            auditLogger.suspiciousActivity(
                                    "unknown",
                                    request.getRemoteAddr(),
                                    "FORBIDDEN_ACCESS: " + request.getRequestURI()
                            );

                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");

                            Map<String, Object> body = Map.of(
                                    "success", false,
                                    "message", "You don't have permission to perform this action.",
                                    "timestamp", java.time.LocalDateTime.now().toString()
                            );

                            new ObjectMapper()
                                    .writeValue(response.getWriter(), body);
                        })
                )

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://*.khoj.com"
        ));
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-Request-ID"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}