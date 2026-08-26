package com.sp.api.comment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateCommentRequest {

    @NotBlank
    private String content;

    /** 답글이면 원 댓글 id. 새 댓글이면 비워 둔다. */
    private Long parentId;
}
