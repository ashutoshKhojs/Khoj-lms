package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import com.khoj.lms.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "instructor_applications")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InstructorApplication extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String expertise;          // "AI, Python, Data Science"

    @Column(nullable = false, length = 1000)
    private String motivation;         // Why they want to teach

    private String linkedinUrl;
    private String portfolioUrl;
    private String experience;         // "3 years teaching Python"
    private String qualifications;     // "B.Tech CSE, IIT Delhi"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    // Admin review fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private String rejectionReason;

    @Column(name = "reviewed_at")
    private java.time.LocalDateTime reviewedAt;
}