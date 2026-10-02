package com.sp.api.chat.dto;

import jakarta.validation.constraints.NotNull;

public record PinRequest(@NotNull(message = "고정할 메시지가 필요합니다.") Long messageId) {
}
