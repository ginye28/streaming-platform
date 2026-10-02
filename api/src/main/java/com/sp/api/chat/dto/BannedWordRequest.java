package com.sp.api.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BannedWordRequest(@NotBlank @Size(max = 30) String word) {
}
