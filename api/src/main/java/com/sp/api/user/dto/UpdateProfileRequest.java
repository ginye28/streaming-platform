package com.sp.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateProfileRequest {

    @NotBlank
    @Size(min = 2, max = 20)
    private String nickname;

    /** 업로드 API 가 돌려준 URL. 비우면 기본 이미지로 되돌린다. */
    private String profileImage;
}
