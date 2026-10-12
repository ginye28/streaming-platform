package com.sp.api.common.ratelimit;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 요청 속도 제한 설정. 값은 모두 "IP(또는 계정) 하나당" 이다.
 * 기본값은 사람이 평범하게 쓸 때는 걸리지 않고, 스크립트로 두드리면 걸리는 정도로 잡았다.
 */
@Getter
@Component
public class RateLimitProperties {

    /** 끄면 아무것도 막지 않는다. 테스트와 로컬 개발에서만 끈다. */
    private final boolean enabled;

    /** 로그인·가입·토큰 갱신. 비밀번호를 대입하는 공격을 늦춘다. */
    private final int authPerMinute;

    /** 같은 이메일로 로그인을 시도하는 횟수(IP 를 바꿔 가며 두드려도 한 계정에는 이만큼만). */
    private final int loginPerEmail;

    private final Duration loginPerEmailWindow;

    /** 글쓰기·좋아요·구독처럼 데이터를 바꾸는 요청. */
    private final int writePerMinute;

    /** 목록·상세 조회. */
    private final int readPerMinute;

    /** 파일 업로드. 디스크와 DB 를 쓰므로 따로 더 적게. */
    private final int uploadPerMinute;

    /** WebSocket 연결(채팅·시청자 수·브라우저 방송). 연결을 계속 만들어 서버를 채우는 것을 막는다. */
    private final int connectPerMinute;

    /** 채팅 한 사람이 10초에 보낼 수 있는 글 수. 방장이 켠 슬로우 모드와 별개로 항상 적용된다. */
    private final int chatPerTenSeconds;

    public RateLimitProperties(
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.auth-per-minute:20}") int authPerMinute,
            @Value("${app.rate-limit.login-per-email:10}") int loginPerEmail,
            @Value("${app.rate-limit.login-per-email-window-minutes:5}") int loginPerEmailWindowMinutes,
            @Value("${app.rate-limit.write-per-minute:120}") int writePerMinute,
            @Value("${app.rate-limit.read-per-minute:600}") int readPerMinute,
            @Value("${app.rate-limit.upload-per-minute:20}") int uploadPerMinute,
            @Value("${app.rate-limit.connect-per-minute:60}") int connectPerMinute,
            @Value("${app.rate-limit.chat-per-ten-seconds:8}") int chatPerTenSeconds
    ) {
        this.enabled = enabled;
        this.authPerMinute = authPerMinute;
        this.loginPerEmail = loginPerEmail;
        this.loginPerEmailWindow = Duration.ofMinutes(loginPerEmailWindowMinutes);
        this.writePerMinute = writePerMinute;
        this.readPerMinute = readPerMinute;
        this.uploadPerMinute = uploadPerMinute;
        this.connectPerMinute = connectPerMinute;
        this.chatPerTenSeconds = chatPerTenSeconds;
    }
}
