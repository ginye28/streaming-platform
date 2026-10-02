package com.sp.api.chat.event;

import com.sp.api.chat.dto.ChatMessageResponse;

/**
 * 채팅 메시지 말고 채팅방에 알려야 하는 일. /topic/lives/{liveId}/events 로 나간다.
 *
 * type 마다 쓰는 칸이 다르고 나머지는 null 이다.
 * <ul>
 *   <li>DELETE — messageId 의 메시지를 화면에서 지운다</li>
 *   <li>PURGE — userId 가 이 방송에서 쓴 메시지를 전부 지운다</li>
 *   <li>PIN — message 를 채팅창 위에 고정한다. null 이면 고정을 푼다</li>
 *   <li>SETTINGS — 슬로우 모드와 채팅 가능 대상이 바뀌었다</li>
 * </ul>
 */
public record ChatEvent(
        String type,
        Long messageId,
        Long userId,
        ChatMessageResponse message,
        Integer slowModeSeconds,
        String chatAudience
) {

    public static ChatEvent deleted(Long messageId) {
        return new ChatEvent("DELETE", messageId, null, null, null, null);
    }

    public static ChatEvent purged(Long userId) {
        return new ChatEvent("PURGE", null, userId, null, null, null);
    }

    public static ChatEvent pinned(ChatMessageResponse message) {
        return new ChatEvent("PIN", null, null, message, null, null);
    }

    public static ChatEvent settings(int slowModeSeconds, String chatAudience) {
        return new ChatEvent("SETTINGS", null, null, null, slowModeSeconds, chatAudience);
    }
}
