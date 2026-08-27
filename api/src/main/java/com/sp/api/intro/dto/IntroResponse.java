package com.sp.api.intro.dto;

/**
 * 방송에 처음 들어온 사람에게 보여 줄 내용.
 *
 * showGate 가 false 면 화면은 인트로를 건너뛰고 바로 방송을 튼다.
 * 누구에게 띄울지는 서버가 정한다 — 규칙이 화면마다 흩어지면 금방 어긋난다.
 */
public record IntroResponse(
        Long channelId,
        String nickname,
        String profileImage,
        /** 있으면 영상으로, 없으면 아래 값들로 카드를 만든다. */
        String videoUrl,
        String headline,
        String greeting,
        long subscriberCount,
        boolean subscribed,
        boolean showGate
) {
}
