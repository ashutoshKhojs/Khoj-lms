package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A practical assignment attached to a lesson (lessonType = ASSIGNMENT).
 *
 * Students submit work (text or file upload via S3).
 * Instructors review and grade manually.
 *
 * Table: assignments
 */
@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "max_marks", nullable = false)
    @Builder.Default
    private Integer maxMarks = 100;

    @Column(name = "passing_marks", nullable = false)
    @Builder.Default
    private Integer passingMarks = 40;

    /** Due date from enrollment date (in days). 0 = no deadline */
    @Column(name = "due_days_from_enrollment", nullable = false)
    @Builder.Default
    private Integer dueDaysFromEnrollment = 0;

    @Column(name = "allow_file_submission", nullable = false)
    @Builder.Default
    private Boolean allowFileSubmission = true;

    @Column(name = "allow_text_submission", nullable = false)
    @Builder.Default
    private Boolean allowTextSubmission = true;

    /** Accepted file types e.g. ".pdf,.zip,.py" */
    @Column(name = "allowed_file_types", length = 200)
    private String allowedFileTypes;

    @Column(name = "max_file_size_mb")
    @Builder.Default
    private Integer maxFileSizeMb = 10;

    @OneToMany(mappedBy = "assignment", fetch = FetchType.LAZY)
    @Builder.Default
    private List<AssignmentSubmission> submissions = new ArrayList<>();
}