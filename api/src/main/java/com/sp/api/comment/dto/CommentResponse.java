package com.sp.api.comment.dto;

import com.sp.api.comment.entity.Comment;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class CommentResponse {

    private Long id;
    private String content;
    private String nickname;
    private LocalDateTime createdAt;

    /** 답글이면 원 댓글 id, 아니면 null. */
    private Long parentId;

    /** 이 댓글에 달린 답글. 답글에는 항상 빈 목록이다 (두 단계까지만 허용). */
    private List<CommentResponse> replies;

    private CommentResponse(Comment comment, Long parentId, List<CommentResponse> replies) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.nickname = comment.getUser().getNickname();
        this.createdAt = comment.getCreatedAt();
        this.parentId = parentId;
        this.replies = replies;
    }

    /** 답글 하나. */
    public static CommentResponse reply(Comment comment, Long parentId) {
        return new CommentResponse(comment, parentId, List.of());
    }

    /** 원 댓글과 거기 달린 답글들. */
    public static CommentResponse withReplies(Comment comment, List<CommentResponse> replies) {
        return new CommentResponse(comment, null, replies);
    }

    /** 답글인지 아닌지 저장된 값에 따라 알아서 만든다. */
    public static CommentResponse of(Comment comment) {
        return new CommentResponse(comment, comment.getParentId(), List.of());
    }
}
