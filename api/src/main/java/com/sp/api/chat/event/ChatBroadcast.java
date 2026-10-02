package com.sp.api.chat.event;

/** 채팅방으로 내보낼 메시지 한 건. 커밋이 끝난 뒤에 나가도록 이벤트로 감싼다. */
public record ChatBroadcast(String destination, Object payload) {
}
