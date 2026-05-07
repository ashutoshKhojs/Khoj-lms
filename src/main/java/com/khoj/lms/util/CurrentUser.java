package com.khoj.lms.util;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * Shorthand for @AuthenticationPrincipal.
 *
 * Usage in controller:
 *   public ResponseEntity<?> create(@CurrentUser UserDetails user, ...) {}
 *
 * Cleaner than repeating @AuthenticationPrincipal everywhere.
 */
@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}