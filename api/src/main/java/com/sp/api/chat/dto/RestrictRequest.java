package com.sp.api.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 채팅 제한 요청.
 *
 * @param minutes 일시 정지 시간(분). 비우거나 0 이하면 풀어 줄 때까지 막는다(강퇴)
 * @param purge   true 면 이 방송에서 그 사람이 쓴 메시지도 모두 지운다. 비우면 지우지 않는다
 */
public record RestrictRequest(
        @NotNull(message = "제한할 사람이 필요합니다.") Long userId,
        Integer minutes,
        @Size(max = 100) String reason,
        Boolean purge
) {
}
