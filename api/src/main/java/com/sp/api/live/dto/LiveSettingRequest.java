package com.sp.api.live.dto;

import com.sp.api.live.entity.Audience;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LiveSettingRequest {

    @NotBlank
    @Size(max = 100)
    private String title;

    private String description;

    private String thumbnailUrl;

    /** 비우면 누구나. */
    private Audience audience;

    /** 비우면 누구나. */
    private Audience chatAudience;

    /** 슬로우 모드 대기 시간(초). 비우면 0(끔). 고를 수 있는 값은 서비스가 다시 확인한다. */
    @Min(0)
    @Max(60)
    private Integer slowModeSeconds;
}
