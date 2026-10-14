package com.sp.api.common.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 키(IP, 이메일, 사용자)별로 "최근 window 안에 몇 번 했는지"를 세는 메모리 제한기.
 *
 * 서버 하나로 도는 지금 구조(슬로우 모드·시청자 수와 같은 가정)에서만 맞다.
 * 서버를 여러 대로 늘리면 바깥 저장소(Redis)로 옮겨야 한다.
 */
public class SlidingWindowRateLimiter {

    /** 키가 이만큼 쌓이면 지난 기록을 한 번 쓸어 낸다. 메모리가 끝없이 커지지 않게 하려는 것이다. */
    private static final int SWEEP_THRESHOLD = 10_000;

    private final int max;
    private final long windowMillis;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(int max, long windowMillis) {
        this.max = max;
        this.windowMillis = windowMillis;
    }

    /** 이번 시도를 기록하고, 한도 안이면 true. 한도를 넘으면 기록하지 않고 false. */
    public boolean tryAcquire(String key) {
        return tryAcquire(key, System.currentTimeMillis());
    }

    boolean tryAcquire(String key, long now) {

        sweepIfLarge(now);

        Deque<Long> deque = hits.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (deque) {
            evictOld(deque, now);

            if (deque.size() >= max) {
                return false;
            }

            deque.addLast(now);
            return true;
        }
    }

    /** 한도를 넘었는지만 본다(기록하지 않는다). 실패 횟수만 세고 싶을 때 {@link #record} 와 함께 쓴다. */
    public boolean isBlocked(String key) {
        return isBlocked(key, System.currentTimeMillis());
    }

    boolean isBlocked(String key, long now) {

        Deque<Long> deque = hits.get(key);

        if (deque == null) {
            return false;
        }

        synchronized (deque) {
            evictOld(deque, now);
            return deque.size() >= max;
        }
    }

    /** 한도와 상관없이 한 번 했다고 기록한다. */
    public void record(String key) {

        long now = System.currentTimeMillis();

        sweepIfLarge(now);

        Deque<Long> deque = hits.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (deque) {
            evictOld(deque, now);
            deque.addLast(now);
        }
    }

    /** 이 키의 기록을 지운다(로그인에 성공하면 실패 횟수를 비운다). */
    public void reset(String key) {
        hits.remove(key);
    }

    /** 다시 시도할 수 있기까지 남은 시간(초, 올림). 막혀 있지 않으면 0. */
    public long retryAfterSeconds(String key) {

        Deque<Long> deque = hits.get(key);

        if (deque == null) {
            return 0;
        }

        long now = System.currentTimeMillis();

        synchronized (deque) {
            evictOld(deque, now);

            if (deque.size() < max || deque.isEmpty()) {
                return 0;
            }

            long waitMillis = windowMillis - (now - deque.peekFirst());

            return Math.max(1, (waitMillis + 999) / 1000);
        }
    }

    private void evictOld(Deque<Long> deque, long now) {
        while (!deque.isEmpty() && now - deque.peekFirst() >= windowMillis) {
            deque.pollFirst();
        }
    }

    private void sweepIfLarge(long now) {

        if (hits.size() < SWEEP_THRESHOLD) {
            return;
        }

        hits.entrySet().removeIf(entry -> {
            Deque<Long> deque = entry.getValue();

            synchronized (deque) {
                evictOld(deque, now);
                return deque.isEmpty();
            }
        });
    }
}
