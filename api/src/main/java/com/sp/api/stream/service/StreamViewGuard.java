package com.sp.api.stream.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 같은 사람이 같은 영상을 짧은 시간에 여러 번 열어도 조회수를 한 번만 올린다.
 * 새로고침을 반복하는 것만으로 숫자가 부풀지 않게 하는 것이 목적이다.
 *
 * 기록은 서버 메모리에만 있다. 서버를 다시 켜면 창이 초기화되고,
 * 서버가 여러 대면 대마다 따로 센다. 동시 시청자 수 집계와 같은 한계이고,
 * Redis 를 들이면 함께 옮겨 갈 자리다.
 */
@Component
public class StreamViewGuard {

    /** 같은 시청자가 다시 열어도 세지 않을 시간. */
    private static final Duration WINDOW = Duration.ofMinutes(30);

    /** 이 수를 넘으면 만료된 기록을 쓸어낸다. 무한정 쌓이는 것만 막으면 된다. */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, Instant> countedAt = new ConcurrentHashMap<>();
    private final Clock clock;

    public StreamViewGuard(Clock clock) {
        this.clock = clock;
    }

    /**
     * 조회수를 올려야 하면 true, 창 안에서 다시 연 것이면 false.
     *
     * 창은 "센 시점"부터 흐른다. 계속 새로고침한다고 해서 창이 밀리지는 않으므로
     * 30분에 한 번은 정상적으로 집계된다.
     */
    public boolean shouldCount(Long streamId, String viewerKey) {

        Instant now = clock.instant();

        if (countedAt.size() > CLEANUP_THRESHOLD) {
            evictExpired(now);
        }

        AtomicBoolean counted = new AtomicBoolean(false);

        countedAt.compute(streamId + "|" + viewerKey, (key, previous) -> {

            if (previous == null || isExpired(previous, now)) {
                counted.set(true);
                return now;
            }

            return previous;
        });

        return counted.get();
    }

    private void evictExpired(Instant now) {
        countedAt.values().removeIf(countedInstant -> isExpired(countedInstant, now));
    }

    private boolean isExpired(Instant countedInstant, Instant now) {
        return countedInstant.isBefore(now.minus(WINDOW));
    }
}
