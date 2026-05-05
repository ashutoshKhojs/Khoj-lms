package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Extended profile information for a user.
 * Separated from User to keep the users table lean.
 * Loaded lazily — only needed on profile page, not on every request.
 *
 * Table: user_profiles
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ─────────────────────────────────────────
    // Academic / Professional Background
    // ─────────────────────────────────────────

    @Column(name = "education_level", length = 100)
    private String educationLevel;          // e.g. "12th Pass", "B.Tech"

    @Column(name = "college_name", length = 150)
    private String collegeName;

    @Column(name = "current_job_title", length = 100)
    private String currentJobTitle;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    // ─────────────────────────────────────────
    // Location (important for Khoj's India-focused mission)
    // ─────────────────────────────────────────

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    // ─────────────────────────────────────────
    // Social / Portfolio Links
    // ─────────────────────────────────────────

    @Column(name = "linkedin_url", length = 300)
    private String linkedinUrl;

    @Column(name = "github_url", length = 300)
    private String githubUrl;

    @Column(name = "portfolio_url", length = 300)
    private String portfolioUrl;

    // ─────────────────────────────────────────
    // Learning Preferences
    // ─────────────────────────────────────────

    /** Preferred language for content: "Hindi", "English" etc. */
    @Column(name = "preferred_language", length = 50)
    @Builder.Default
    private String preferredLanguage = "English";

    /** Used to serve low-res videos and lighter pages */
    @Column(name = "is_low_bandwidth_mode", nullable = false)
    @Builder.Default
    private Boolean isLowBandwidthMode = false;

    // ─────────────────────────────────────────
    // Instructor-specific (populated when user is INSTRUCTOR)
    // ─────────────────────────────────────────

    @Column(name = "expertise_areas", columnDefinition = "TEXT")
    private String expertiseAreas;          // JSON array or comma-separated

    @Column(name = "instructor_bio", columnDefinition = "TEXT")
    private String instructorBio;

    @Column(name = "is_instructor_approved", nullable = false)
    @Builder.Default
    private Boolean isInstructorApproved = false;

    @Column(name = "instructor_approval_note", length = 500)
    private String instructorApprovalNote;
}