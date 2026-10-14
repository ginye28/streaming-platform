package com.sp.api.common.ratelimit;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 일반 요청 속도 제한 설정. 값은 모두 "접속 IP 하나당 분당" 이다.
 * 기본값은 사람이 평범하게 쓸 때는 걸리지 않고, 스크립트로 두드리면 걸리는 정도로 잡았다.
 *
 * 로그인·가입·토큰 갱신(IP 별)과 계정별 로그인 실패 잠금, 채팅 도배는 {@code AuthRateLimitFilter}·
 * {@code AuthService}·{@code ChatFloodGuard} 가 따로 한다(docs/09-security.md). 같은 스위치
 * ({@code app.security.rate-limit.enabled})로 함께 켜고 끈다.
 */
@Getter
@Component
public class RateLimitProperties {

    /** 끄면 아무것도 막지 않는다. 테스트와 로컬 개발에서만 끈다. */
    private final boolean enabled;

    /** 글쓰기·좋아요·구독처럼 데이터를 바꾸는 요청. */
    private final int writePerMinute;

    /** 목록·상세 조회. */
    private final int readPerMinute;

    /** 파일 업로드. 디스크와 DB 를 쓰므로 따로 더 적게. */
    private final int uploadPerMinute;

    /** WebSocket 연결(채팅·시청자 수·브라우저 방송). 연결을 계속 만들어 서버를 채우는 것을 막는다. */
    private final int connectPerMinute;

    public RateLimitProperties(
            @Value("${app.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.security.rate-limit.write-per-minute:120}") int writePerMinute,
            @Value("${app.security.rate-limit.read-per-minute:600}") int readPerMinute,
            @Value("${app.security.rate-limit.upload-per-minute:20}") int uploadPerMinute,
            @Value("${app.security.rate-limit.connect-per-minute:60}") int connectPerMinute
    ) {
        this.enabled = enabled;
        this.writePerMinute = writePerMinute;
        this.readPerMinute = readPerMinute;
        this.uploadPerMinute = uploadPerMinute;
        this.connectPerMinute = connectPerMinute;
    }
}
