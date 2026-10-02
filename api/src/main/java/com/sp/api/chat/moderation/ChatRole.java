package com.sp.api.chat.moderation;

/** 채널 안에서 채팅 운영 권한을 가진 역할. 일반 시청자는 역할이 없다(null). */
public enum ChatRole {
    /** 채널 주인. */
    OWNER,
    /** 주인이 맡긴 매니저. */
    MANAGER
}
