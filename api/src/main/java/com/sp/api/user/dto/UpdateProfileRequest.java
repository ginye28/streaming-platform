package com.sp.api.user.dto;

import com.sp.api.common.validation.SafeUrl;
import com.sp.api.common.validation.SafeText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateProfileRequest {

    @NotBlank
    @Size(min = 2, max = 20)
    @SafeText
    private String nickname;

    /** 업로드 API 가 돌려준 URL. 비우면 기본 이미지로 되돌린다. */
    @Size(max = 255)
    @SafeUrl
    private String profileImage;
}
