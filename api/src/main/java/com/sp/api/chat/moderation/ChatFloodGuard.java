package com.sp.api.chat.moderation;

import com.sp.api.common.security.SlidingWindowRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 슬로우 모드를 꺼 둔 방송에서도 한 사람이 채팅을 도배하지 못하게 하는 바닥 한도.
 * 채널 주인·매니저는 {@link ChatPolicy} 가 앞에서 건너뛰므로 여기까지 오지 않는다.
 */
@Component
public class ChatFloodGuard {

    private final boolean enabled;
    private final SlidingWindowRateLimiter limiter;

    public ChatFloodGuard(
            @Value("${app.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.chat.flood-max:10}") int max,
            @Value("${app.chat.flood-window-seconds:10}") int windowSeconds
    ) {
        this.enabled = enabled;
        this.limiter = new SlidingWindowRateLimiter(max, windowSeconds * 1000L);
    }

    /** 지금 보내도 되면 true. 한도를 넘으면 false. */
    public boolean tryAcquire(Long userId) {
        return !enabled || limiter.tryAcquire(String.valueOf(userId));
    }
}
