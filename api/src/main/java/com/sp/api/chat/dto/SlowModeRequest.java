package com.sp.api.chat.dto;

import jakarta.validation.constraints.NotNull;

/** 슬로우 모드 대기 시간(초). 0 이면 끈다. 고를 수 있는 값은 서버가 정한다. */
public record SlowModeRequest(@NotNull Integer seconds) {
}
