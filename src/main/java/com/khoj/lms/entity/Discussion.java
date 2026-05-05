package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A question or discussion post within a course (or a specific lesson).
 *
 * Students ask questions; instructors and peers reply.
 * Supports upvotes and instructor-marked resolution.
 *
 * Table: discussions
 */
@Entity
@Table(
        name = "discussions",
        indexes = {
                @Index(name = "idx_discussion_course",  columnList = "course_id"),
                @Index(name = "idx_discussion_lesson",  columnList = "lesson_id"),
                @Index(name = "idx_discussion_author",  columnList = "author_id"),
                @Index(name = "idx_discussion_resolved",columnList = "is_resolved")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discussion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * Optional — if set, the question is about a specific lesson.
     * If null, it's a general course-level discussion.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "upvote_count", nullable = false)
    @Builder.Default
    private Integer upvoteCount = 0;

    @Column(name = "reply_count", nullable = false)
    @Builder.Default
    private Integer replyCount = 0;

    @Column(name = "is_resolved", nullable = false)
    @Builder.Default
    private Boolean isResolved = false;

    /** The reply that solved the question (marked by author or instructor) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_reply_id")
    private DiscussionReply acceptedReply;

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false;

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = false;

    @OneToMany(mappedBy = "discussion", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<DiscussionReply> replies = new ArrayList<>();

    public void markResolved(DiscussionReply acceptedReply) {
        this.isResolved = true;
        this.acceptedReply = acceptedReply;
    }

    public void incrementReplyCount() {
        this.replyCount++;
    }
}