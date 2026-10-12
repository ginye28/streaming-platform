package com.sp.api.comment.dto;

import com.sp.api.common.validation.SafeText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateCommentRequest {

    @NotBlank
    @Size(max = 1000, message = "댓글은 1000자 이하여야 합니다.")
    @SafeText
    private String content;
}