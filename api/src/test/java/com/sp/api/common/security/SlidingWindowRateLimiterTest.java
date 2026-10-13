package com.sp.api.common.security;

import com.sp.api.common.validation.HttpUrlValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlidingWindowRateLimiterTest {

    @Test
    @DisplayName("한도까지는 되고, 넘으면 막히며, 창이 지나면 다시 된다")
    void blocksOverLimitAndRecoversAfterWindow() {

        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(3, 1_000);

        assertThat(limiter.tryAcquire("a", 0)).isTrue();
        assertThat(limiter.tryAcquire("a", 100)).isTrue();
        assertThat(limiter.tryAcquire("a", 200)).isTrue();
        assertThat(limiter.tryAcquire("a", 300)).isFalse();

        // 첫 기록(0ms)이 창(1000ms) 밖으로 나가면 한 번이 비어 다시 된다.
        assertThat(limiter.tryAcquire("a", 1_000)).isTrue();
        assertThat(limiter.tryAcquire("a", 1_050)).isFalse();
    }

    @Test
    @DisplayName("키마다 따로 센다")
    void countsPerKey() {

        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(1, 1_000);

        assertThat(limiter.tryAcquire("a", 0)).isTrue();
        assertThat(limiter.tryAcquire("b", 0)).isTrue();
        assertThat(limiter.tryAcquire("a", 1)).isFalse();
    }

    @Test
    @DisplayName("실패만 세는 쓰임: record 로 쌓고 isBlocked 로 보며 reset 으로 비운다")
    void recordBlockAndReset() {

        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(2, 60_000);

        assertThat(limiter.isBlocked("u")).isFalse();

        limiter.record("u");
        assertThat(limiter.isBlocked("u")).isFalse();

        limiter.record("u");
        assertThat(limiter.isBlocked("u")).isTrue();
        assertThat(limiter.retryAfterSeconds("u")).isPositive();

        limiter.reset("u");
        assertThat(limiter.isBlocked("u")).isFalse();
    }

    @Test
    @DisplayName("http(s) 주소만 통과하고, 스크립트·상대·공백 섞인 주소는 거절한다")
    void httpUrlValidator() {

        assertThat(HttpUrlValidator.isHttpUrl("https://example.com/a?b=1")).isTrue();
        assertThat(HttpUrlValidator.isHttpUrl("http://example.com")).isTrue();
        assertThat(HttpUrlValidator.isHttpUrl("HTTPS://EXAMPLE.COM")).isTrue();

        assertThat(HttpUrlValidator.isHttpUrl("javascript:alert(1)")).isFalse();
        assertThat(HttpUrlValidator.isHttpUrl("JaVaScRiPt:alert(1)")).isFalse();
        assertThat(HttpUrlValidator.isHttpUrl(" javascript:alert(1)")).isFalse();
        assertThat(HttpUrlValidator.isHttpUrl("java\tscript:alert(1)")).isFalse();
        assertThat(HttpUrlValidator.isHttpUrl("data:text/html,hi")).isFalse();
        assertThat(HttpUrlValidator.isHttpUrl("//evil.example/x")).isFalse();
        assertThat(HttpUrlValidator.isHttpUrl("/relative/path")).isFalse();
        assertThat(HttpUrlValidator.isHttpUrl("https://")).isFalse();
        assertThat(HttpUrlValidator.isHttpUrl("example.com")).isFalse();
    }
}
