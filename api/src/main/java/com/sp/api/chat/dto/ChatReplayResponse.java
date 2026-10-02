package com.sp.api.chat.dto;

import java.util.List;

/**
 * 다시보기 채팅 한 묶음.
 *
 * @param messages    오래된 순. 방송 시작 뒤 몇 초째인지는 각 메시지의 offsetSeconds 에 있다
 * @param nextAfterId 더 받을 게 있으면 다음 요청의 afterId. 끝이면 null
 */
public record ChatReplayResponse(List<ChatMessageResponse> messages, Long nextAfterId) {
}
