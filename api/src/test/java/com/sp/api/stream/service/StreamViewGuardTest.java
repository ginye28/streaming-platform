package com.sp.api.stream.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class StreamViewGuardTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private final StreamViewGuard guard = new StreamViewGuard(clock);

    @Test
    @DisplayName("같은 시청자가 새로고침을 반복해도 한 번만 센다")
    void countsOncePerViewer() {

        assertThat(guard.shouldCount(1L, "user:a@test.com")).isTrue();
        assertThat(guard.shouldCount(1L, "user:a@test.com")).isFalse();
        assertThat(guard.shouldCount(1L, "user:a@test.com")).isFalse();
    }

    @Test
    @DisplayName("시청자가 다르면 각각 센다")
    void countsEachViewerSeparately() {

        assertThat(guard.shouldCount(1L, "user:a@test.com")).isTrue();
        assertThat(guard.shouldCount(1L, "user:b@test.com")).isTrue();
        assertThat(guard.shouldCount(1L, "ip:10.0.0.1")).isTrue();
    }

    @Test
    @DisplayName("영상이 다르면 같은 시청자라도 각각 센다")
    void countsEachStreamSeparately() {

        assertThat(guard.shouldCount(1L, "user:a@test.com")).isTrue();
        assertThat(guard.shouldCount(2L, "user:a@test.com")).isTrue();
    }

    @Test
    @DisplayName("창이 지나면 다시 센다")
    void countsAgainAfterWindow() {

        assertThat(guard.shouldCount(1L, "user:a@test.com")).isTrue();

        clock.advance(Duration.ofMinutes(29));
        assertThat(guard.shouldCount(1L, "user:a@test.com")).isFalse();

        clock.advance(Duration.ofMinutes(2));
        assertThat(guard.shouldCount(1L, "user:a@test.com")).isTrue();
    }

    @Test
    @DisplayName("중간에 계속 새로고침해도 창이 밀리지 않는다")
    void windowStartsWhenCounted() {

        assertThat(guard.shouldCount(1L, "user:a@test.com")).isTrue();

        // 5분마다 다시 열어 본다. 세지 않으므로 창의 시작점도 그대로여야 한다.
        for (int i = 0; i < 5; i++) {
            clock.advance(Duration.ofMinutes(5));
            assertThat(guard.shouldCount(1L, "user:a@test.com")).isFalse();
        }

        // 처음 센 지 31분
        clock.advance(Duration.ofMinutes(6));
        assertThat(guard.shouldCount(1L, "user:a@test.com")).isTrue();
    }

    /** 시간을 앞으로 밀어 볼 수 있는 시계. */
    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
