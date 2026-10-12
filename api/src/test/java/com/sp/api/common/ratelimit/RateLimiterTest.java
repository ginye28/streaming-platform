package com.sp.api.common.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    /** 테스트가 시간을 직접 흘려 보낼 수 있는 시계. */
    private static final class MutableClock extends Clock {

        private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-13T00:00:00Z"));

        void advance(Duration duration) {
            now.updateAndGet(current -> current.plus(duration));
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }

    private final MutableClock clock = new MutableClock();
    private final RateLimiter limiter = new RateLimiter(clock);

    @Test
    @DisplayName("한도만큼은 받아 주고 넘치면 거절하며, 기다려야 하는 초를 알려 준다")
    void rejectsAfterCapacity() {

        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryConsume("k", 3, Duration.ofMinutes(1)).allowed()).isTrue();
        }

        RateLimiter.Decision rejected = limiter.tryConsume("k", 3, Duration.ofMinutes(1));

        assertThat(rejected.allowed()).isFalse();
        // 3개 / 60초 = 20초에 한 개씩 다시 찬다
        assertThat(rejected.retryAfterSeconds()).isEqualTo(20);
    }

    @Test
    @DisplayName("시간이 지나면 다시 찬다")
    void refillsOverTime() {

        for (int i = 0; i < 3; i++) {
            limiter.tryConsume("k", 3, Duration.ofMinutes(1));
        }

        assertThat(limiter.tryConsume("k", 3, Duration.ofMinutes(1)).allowed()).isFalse();

        clock.advance(Duration.ofSeconds(21));

        assertThat(limiter.tryConsume("k", 3, Duration.ofMinutes(1)).allowed()).isTrue();
        assertThat(limiter.tryConsume("k", 3, Duration.ofMinutes(1)).allowed()).isFalse();
    }

    @Test
    @DisplayName("키가 다르면 서로 영향을 주지 않는다")
    void keysAreIndependent() {

        limiter.tryConsume("a", 1, Duration.ofMinutes(1));

        assertThat(limiter.tryConsume("a", 1, Duration.ofMinutes(1)).allowed()).isFalse();
        assertThat(limiter.tryConsume("b", 1, Duration.ofMinutes(1)).allowed()).isTrue();
    }

    @Test
    @DisplayName("오래 비어 있어도 capacity 보다 더 쌓이지 않는다")
    void doesNotAccumulateBeyondCapacity() {

        limiter.tryConsume("k", 2, Duration.ofMinutes(1));

        clock.advance(Duration.ofHours(5));

        assertThat(limiter.tryConsume("k", 2, Duration.ofMinutes(1)).allowed()).isTrue();
        assertThat(limiter.tryConsume("k", 2, Duration.ofMinutes(1)).allowed()).isTrue();
        assertThat(limiter.tryConsume("k", 2, Duration.ofMinutes(1)).allowed()).isFalse();
    }

    @Test
    @DisplayName("남은 횟수를 돌려준다")
    void reportsRemaining() {

        assertThat(limiter.tryConsume("k", 5, Duration.ofMinutes(1)).remaining()).isEqualTo(4);
        assertThat(limiter.tryConsume("k", 5, Duration.ofMinutes(1)).remaining()).isEqualTo(3);
    }
}
