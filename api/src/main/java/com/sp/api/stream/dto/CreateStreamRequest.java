package com.sp.api.stream.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateStreamRequest {

    @NotBlank
    private String title;

    private String description;

    private String thumbnailUrl;

    @NotBlank
    private String videoUrl;
}