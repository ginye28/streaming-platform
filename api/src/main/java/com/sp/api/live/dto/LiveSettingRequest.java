package com.sp.api.live.dto;

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
}
