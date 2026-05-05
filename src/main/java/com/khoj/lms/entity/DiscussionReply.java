package com.khoj.lms.entity;

import com.khoj.lms.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * A reply to a Discussion post.
 * Can be posted by a student, instructor, or admin.
 * isInstructorReply flag highlights official instructor responses.
 *
 * Table: discussion_replies
 */
@Entity
@Table(
        name = "discussion_replies",
        indexes = {
                @Index(name = "idx_reply_discussion", columnList = "discussion_id"),
                @Index(name = "idx_reply_author",     columnList = "author_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscussionReply extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discussion_id", nullable = false)
    private Discussion discussion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "upvote_count", nullable = false)
    @Builder.Default
    private Integer upvoteCount = 0;

    /** Highlights this reply as an official instructor response in the UI */
    @Column(name = "is_instructor_reply", nullable = false)
    @Builder.Default
    private Boolean isInstructorReply = false;

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = false;

    /** Marks this reply as the accepted answer (set via Discussion.acceptedReply) */
    @Column(name = "is_accepted_answer", nullable = false)
    @Builder.Default
    private Boolean isAcceptedAnswer = false;
}