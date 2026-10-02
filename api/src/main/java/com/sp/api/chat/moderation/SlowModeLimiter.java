package com.sp.api.chat.moderation;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 슬로우 모드: 한 사람이 같은 방송에서 채팅을 다시 보내기까지 기다리게 한다.
 *
 * 메모리에만 둔다. 서버 하나로 도는 지금 구조(단순 인메모리 채팅 브로커)와 같은 가정이고,
 * 서버를 여러 대로 늘리면 채팅 브로커와 함께 바깥 저장소로 옮겨야 한다.
 */
@Component
public class SlowModeLimiter {

    private final Map<Long, Map<Long, Long>> lastSentMillis = new ConcurrentHashMap<>();

    /**
     * 지금 보내도 되는지 묻고, 되면 지금 보낸 것으로 기록한다.
     *
     * @return 0 이면 보내도 된다. 아니면 더 기다려야 하는 시간(초, 올림)
     */
    public long tryAcquire(Long liveId, Long userId, int seconds) {

        long now = System.currentTimeMillis();
        long window = seconds * 1000L;

        long[] wait = {0};

        lastSentMillis
                .computeIfAbsent(liveId, key -> new ConcurrentHashMap<>())
                .compute(userId, (key, last) -> {

                    if (last != null && now - last < window) {
                        wait[0] = (window - (now - last) + 999) / 1000;
                        return last;
                    }

                    return now;
                });

        return wait[0];
    }

    /** 방송이 끝나면 그 방송의 기록을 버린다. */
    public void clear(Long liveId) {
        lastSentMillis.remove(liveId);
    }
}
