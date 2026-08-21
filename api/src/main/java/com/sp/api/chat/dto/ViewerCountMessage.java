package com.sp.api.chat.dto;

/** 시청자 수가 바뀔 때 채팅방에 함께 밀어 주는 메시지. */
public record ViewerCountMessage(Long liveId, long viewerCount) {
}
