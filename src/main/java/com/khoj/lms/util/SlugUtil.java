package com.khoj.lms.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Generates URL-safe slugs from strings.
 *
 * Examples:
 *   "AI Basics for Beginners!"  → "ai-basics-for-beginners"
 *   "Python 3.x & Data Science" → "python-3x-data-science"
 *   "Web Dev (2024)"            → "web-dev-2024"
 */
public final class SlugUtil {

    private static final Pattern NON_LATIN      = Pattern.compile("[^\\w-]");
    private static final Pattern MULTI_HYPHEN   = Pattern.compile("-{2,}");
    private static final Pattern LEADING_TRAIL  = Pattern.compile("^-|-$");

    private SlugUtil() {}

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) return "";

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", ""); // strip accents

        return NON_LATIN.matcher(
                        normalized.toLowerCase()
                                .trim()
                                .replace(" ", "-")
                                .replace("_", "-")
                )
                .replaceAll("")
                .transform(s -> MULTI_HYPHEN.matcher(s).replaceAll("-"))
                .transform(s -> LEADING_TRAIL.matcher(s).replaceAll(""));
    }

    /**
     * Makes slug unique by appending a suffix if it already exists.
     * Example: "ai-basics" → "ai-basics-2" → "ai-basics-3"
     */
    public static String makeUnique(String baseSlug, java.util.function.Predicate<String> existsCheck) {
        if (!existsCheck.test(baseSlug)) return baseSlug;
        int counter = 2;
        while (existsCheck.test(baseSlug + "-" + counter)) counter++;
        return baseSlug + "-" + counter;
    }
}