package com.khoj.lms.config;

import com.khoj.lms.security.JwtAuthFilter;
import com.khoj.lms.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

/**
 * Spring Security configuration for Khoj LMS.
 *
 * Strategy:
 *  - STATELESS sessions (JWT — no server-side sessions)
 *  - CSRF disabled (SPA with JWT doesn't need it)
 *  - Public routes: /auth/**, course listings, certificate verify
 *  - Role-based access via @PreAuthorize on service/controller methods
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)   // Enables @PreAuthorize / @PostAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    // ─────────────────────────────────────────
    // Public routes — no token needed
    // ─────────────────────────────────────────
    private static final String[] PUBLIC_ROUTES = {
            "/auth/**",
            "/courses",                         // course listing
            "/courses/{slug}",                  // course detail page
            "/categories/**",                   // category listing
            "/certificates/verify/**",          // public cert verification
            "/actuator/health",
            "/v3/api-docs/**",                  // Swagger
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — stateless JWT API doesn't need it
                .csrf(AbstractHttpConfigurer::disable)

                // CORS — allow frontend origin
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Session — completely stateless
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public GET routes
                        .requestMatchers(HttpMethod.GET, PUBLIC_ROUTES).permitAll()
                        // Auth endpoints (POST)
                        .requestMatchers(HttpMethod.POST, "/auth/**").permitAll()
                        // Lesson preview (free preview videos don't need auth)
                        .requestMatchers(HttpMethod.GET, "/lessons/*/preview").permitAll()
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // Plug in our JWT filter before the default username/password filter
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ─────────────────────────────────────────
    // CORS — allow React frontend (adjust origin for production)
    // ─────────────────────────────────────────
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",        // Next.js dev
                "http://localhost:5173",        // Vite dev
                "https://*.khoj.com"            // Production domains
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ─────────────────────────────────────────
    // Beans
    // ─────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);    // Cost factor 12 — good balance for 2024
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService); // ✅ FIX

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}