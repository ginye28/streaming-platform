package com.sp.api.comment.dto;

import com.sp.api.comment.entity.Comment;
import com.sp.api.vtuber.dto.OshiMark;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class CommentResponse {

    private Long id;
    private String content;
    private String nickname;

    /** 이 영상의 채널을 구독한 사람에게만 이름 옆에 붙는 표식. 아니면 null. */
    private String oshiMarkUrl;

    /** 표식이 있을 때 구독 등급(BASIC / PAID). 없으면 null. */
    private String oshiTier;

    private LocalDateTime createdAt;

    /** 답글이면 원 댓글 id, 아니면 null. */
    private Long parentId;

    /** 이 댓글에 달린 답글. 답글에는 항상 빈 목록이다 (두 단계까지만 허용). */
    private List<CommentResponse> replies;

    private CommentResponse(
            Comment comment, Long parentId, List<CommentResponse> replies, OshiMark mark
    ) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.nickname = comment.getUser().getNickname();
        this.oshiMarkUrl = mark == null ? null : mark.url();
        this.oshiTier = mark == null ? null : mark.tier().name();
        this.createdAt = comment.getCreatedAt();
        this.parentId = parentId;
        this.replies = replies;
    }

    /** 답글 하나. */
    public static CommentResponse reply(Comment comment, Long parentId, OshiMark mark) {
        return new CommentResponse(comment, parentId, List.of(), mark);
    }

    /** 원 댓글과 거기 달린 답글들. */
    public static CommentResponse withReplies(
            Comment comment, List<CommentResponse> replies, OshiMark mark
    ) {
        return new CommentResponse(comment, null, replies, mark);
    }

    /** 답글인지 아닌지 저장된 값에 따라 알아서 만든다. */
    public static CommentResponse of(Comment comment, OshiMark mark) {
        return new CommentResponse(comment, comment.getParentId(), List.of(), mark);
    }
}
