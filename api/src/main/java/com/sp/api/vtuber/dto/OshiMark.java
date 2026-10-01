package com.sp.api.vtuber.dto;

import com.sp.api.subscribe.entity.SubscriptionTier;

/**
 * 이름 옆에 붙일 오시마크 한 개.
 *
 * @param url  이미지 주소
 * @param tier 이 사람의 구독 등급. 화면이 일반/유료를 다르게 꾸밀 때 쓴다.
 */
public record OshiMark(String url, SubscriptionTier tier) {
}
