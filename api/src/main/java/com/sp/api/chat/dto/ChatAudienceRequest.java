package com.sp.api.chat.dto;

import com.sp.api.live.entity.Audience;
import jakarta.validation.constraints.NotNull;

public record ChatAudienceRequest(@NotNull Audience chatAudience) {
}
