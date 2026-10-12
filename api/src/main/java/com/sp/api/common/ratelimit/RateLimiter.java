package com.sp.api.common.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 토큰 버킷 방식 요청 속도 제한. 키(IP·계정 등)마다 버킷 하나를 둔다.
 *
 * 버킷은 capacity 만큼 차 있다가 요청 한 번에 한 개씩 줄고, window 동안 capacity 만큼 다시 찬다.
 * 그래서 잠깐 몰리는 요청(capacity 만큼)은 받아 주고, 계속 넘치게 보내는 쪽만 막는다.
 *
 * 메모리에만 둔다. SlowModeLimiter·시청자 수 집계와 같은 가정(서버 한 대)이고,
 * 서버를 여러 대로 늘리면 인스턴스마다 따로 센다. 그때는 Redis 로 옮긴다.
 */
@Component
public class RateLimiter {

    /** 이 개수를 넘게 쌓이면 오래 안 쓴 버킷부터 치운다. 공격자가 키를 무한히 만들어 메모리를 채우지 못하게 한다. */
    private static final int PURGE_THRESHOLD = 20_000;

    /** 이만큼 안 쓴 버킷은 가득 찬 것과 같으니 지워도 된다. */
    private static final long IDLE_MILLIS = Duration.ofMinutes(15).toMillis();

    private final Clock clock;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicInteger calls = new AtomicInteger();

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * 요청 한 번을 쓴다.
     *
     * @param capacity 한꺼번에 받아 주는 최대 요청 수
     * @param window   capacity 만큼 다시 차는 데 걸리는 시간
     */
    public Decision tryConsume(String key, int capacity, Duration window) {

        long now = clock.millis();
        double refillPerMilli = (double) capacity / Math.max(1, window.toMillis());

        maybePurge(now);

        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, now));

        synchronized (bucket) {

            double elapsed = Math.max(0, now - bucket.lastRefillMillis);
            bucket.tokens = Math.min(capacity, bucket.tokens + elapsed * refillPerMilli);
            bucket.lastRefillMillis = now;
            bucket.lastUsedMillis = now;

            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                return new Decision(true, 0, (long) Math.floor(bucket.tokens));
            }

            long waitMillis = (long) Math.ceil((1 - bucket.tokens) / refillPerMilli);

            return new Decision(false, Math.max(1, (waitMillis + 999) / 1000), 0);
        }
    }

    /** 지금 관리 중인 버킷 수. 테스트와 모니터링용. */
    public int size() {
        return buckets.size();
    }

    private void maybePurge(long now) {

        // 요청마다 전체를 훑지 않도록 가끔씩만, 그리고 많이 쌓였을 때만 치운다.
        if (calls.incrementAndGet() % 500 != 0 || buckets.size() < PURGE_THRESHOLD) {
            return;
        }

        buckets.entrySet().removeIf(entry -> now - entry.getValue().lastUsedMillis > IDLE_MILLIS);
    }

    /** @param retryAfterSeconds 거절됐을 때 다시 시도해도 되는 때까지 남은 초(올림) */
    public record Decision(boolean allowed, long retryAfterSeconds, long remaining) {
    }

    private static final class Bucket {

        private double tokens;
        private long lastRefillMillis;
        private long lastUsedMillis;

        private Bucket(double tokens, long now) {
            this.tokens = tokens;
            this.lastRefillMillis = now;
            this.lastUsedMillis = now;
        }
    }
}
