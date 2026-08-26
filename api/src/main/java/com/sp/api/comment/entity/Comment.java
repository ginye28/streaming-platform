package com.sp.api.comment.entity;

import com.sp.api.stream.entity.Stream;
import com.sp.api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comments", indexes = {
        // 원 댓글 목록은 stream_id + parent_id 로, 답글 묶어 오기는 parent_id 로 찾는다.
        @Index(name = "idx_comments_stream_parent", columnList = "stream_id, parent_id"),
        @Index(name = "idx_comments_parent", columnList = "parent_id")
})
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stream_id", nullable = false)
    private Stream stream;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 답글이면 원 댓글, 아니면 null.
     * 답글에 다시 답글을 다는 건 막는다 — 깊이가 늘어나면 화면 들여쓰기와 페이지 계산이 같이 복잡해진다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Comment(String content, Stream stream, User user) {
        this(content, stream, user, null);
    }

    public Comment(String content, Stream stream, User user, Comment parent) {
        this.content = content;
        this.stream = stream;
        this.user = user;
        this.parent = parent;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public boolean isOwnedBy(String email) {
        return user.getEmail().equals(email);
    }

    public boolean isReply() {
        return parent != null;
    }

    /** 답글이면 원 댓글 id, 아니면 null. */
    public Long getParentId() {
        return parent == null ? null : parent.getId();
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
