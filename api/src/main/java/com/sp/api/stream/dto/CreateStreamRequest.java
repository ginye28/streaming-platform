package com.sp.api.stream.dto;

import com.sp.api.common.validation.SafeUrl;
import com.sp.api.common.validation.SafeText;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateStreamRequest {

    @NotBlank
    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    @SafeText
    private String title;

    @Size(max = 2000, message = "설명은 2000자 이하여야 합니다.")
    @SafeText
    private String description;

    @Size(max = 255)
    @SafeUrl
    private String thumbnailUrl;

    @NotBlank
    @Size(max = 255)
    @SafeUrl
    private String videoUrl;

    private Long categoryId;
}