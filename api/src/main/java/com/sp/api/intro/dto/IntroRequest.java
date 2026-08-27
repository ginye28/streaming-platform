package com.sp.api.intro.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IntroRequest {

    /** 짧은 자기소개 영상. 비워 두면 카드로 보여 준다. */
    private String videoUrl;

    @Size(max = 60, message = "한 줄 소개는 60자 이하여야 합니다.")
    private String headline;

    @Size(max = 2000, message = "소개글은 2000자 이하여야 합니다.")
    private String greeting;
}
