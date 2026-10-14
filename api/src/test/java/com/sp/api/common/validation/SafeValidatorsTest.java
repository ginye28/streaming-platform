package com.sp.api.common.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SafeValidatorsTest {

    private final SafeTextValidator text = new SafeTextValidator();
    private final SafeUrlValidator url = new SafeUrlValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert(1)</script>",
            "안녕 <img src=x onerror=alert(1)>",
            "</div>",
            "<!-- 주석 -->",
            "<?xml version=\"1.0\"?>",
            "<a href=\"javascript:alert(1)\">눌러</a>",
            "<SCRIPT\nsrc=//evil></SCRIPT>",
            "널\u0000문자",
            "벨\u0007소리",
            "방향‮뒤집기",
    })
    @DisplayName("태그·제어 문자·방향 조작은 거절한다")
    void rejectsDangerousText(String value) {
        assertThat(text.isValid(value, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "안녕하세요",
            "I <3 you",
            "1 < 2 그리고 3 > 2",
            "a<b",
            "JavaScript: 어렵다",
            "줄바꿈\n두 줄\t탭",
            "😀 이모지 👨‍👩‍👧 가족",
            "Tom & Jerry",
            "\"따옴표\" 와 '작은따옴표'",
    })
    @DisplayName("평범한 글은 부등호·이모지·줄바꿈이 있어도 통과한다")
    void acceptsPlainText(String value) {
        assertThat(text.isValid(value, null)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("비어 있는 값은 통과한다(필수 여부는 @NotBlank)")
    void emptyIsValid(String value) {
        assertThat(text.isValid(value, null)).isTrue();
        assertThat(url.isValid(value, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/uploads/a.mp4",
            "https://example.com/video.mp4",
            "http://localhost:8081/hls/b-1.m3u8",
            "https://abc.trycloudflare.com/hls/x.m3u8?token=1",
    })
    @DisplayName("업로드 경로와 http(s) 주소는 통과한다")
    void acceptsSafeUrl(String value) {
        assertThat(url.isValid(value, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "JaVaScRiPt:alert(1)",
            " javascript:alert(1)",
            "data:text/html;base64,PHNjcmlwdD4=",
            "vbscript:msgbox(1)",
            "file:///etc/passwd",
            "//evil.example.com/x.js",
            "example.com/no-scheme",
            "https://",
            "https://exa mple.com",
            "https://example.com/\u0000",
            "/uploads\\..\\x",
    })
    @DisplayName("위험하거나 모양이 틀린 주소는 거절한다")
    void rejectsUnsafeUrl(String value) {
        assertThat(url.isValid(value, null)).isFalse();
    }
}
