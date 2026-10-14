package com.sp.api.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 비정상으로 보이는 접근을 기록해 두는 곳. 관리자가 "지금 무슨 일이 있는지" 볼 수 있게 한다.
 *
 * 최근 사건 {@value #MAX_EVENTS}개와 종류별 누적 횟수를 메모리에만 둔다(서버를 재시작하면 비워진다).
 * 오래 남겨야 하는 감사 기록이 아니라, 요청이 몰리거나 로그인을 두드릴 때 바로 알아채기 위한 창이다.
 * 같은 내용은 로그(WARN)로도 남아서 Render 로그에서 검색할 수 있다.
 */
@Slf4j
@Component
public class SecurityEventRecorder {

    public static final int MAX_EVENTS = 200;

    public enum Type {
        /** 요청이 너무 많아 429 로 막았다 */
        RATE_LIMITED,
        /** 이메일·비밀번호가 틀린 로그인 */
        LOGIN_FAILED,
        /** 로그인 없이 보호된 주소를 부름(401) */
        UNAUTHORIZED,
        /** 권한이 없는 주소를 부름(403) */
        FORBIDDEN,
        /** HTML 태그·스크립트·제어 문자처럼 입력 필터에 걸려 거절된 글 */
        INPUT_REJECTED
    }

    public record Event(Instant at, Type type, String ip, String path, String detail) {
    }

    public record Snapshot(Instant since, Map<Type, Long> counts, List<Event> recent) {
    }

    private final Clock clock;
    private final Instant since;
    private final Map<Type, Long> counts = new EnumMap<>(Type.class);
    private final Deque<Event> recent = new ArrayDeque<>();

    public SecurityEventRecorder(Clock clock) {
        this.clock = clock;
        this.since = clock.instant();
    }

    public void record(Type type, String ip, String path, String detail) {
        record(type, ip, path, detail, true);
    }

    /**
     * @param store false 면 횟수만 세고 목록·로그에는 남기지 않는다.
     *              한 사람이 수천 번 두드릴 때 로그와 목록이 그 한 사람으로 가득 차는 것을 막는다.
     */
    public void record(Type type, String ip, String path, String detail, boolean store) {

        if (!store) {
            synchronized (this) {
                counts.merge(type, 1L, Long::sum);
            }
            return;
        }

        Event event = new Event(clock.instant(), type, clean(ip, 64), clean(path, 200), clean(detail, 200));

        synchronized (this) {
            counts.merge(type, 1L, Long::sum);

            recent.addFirst(event);

            while (recent.size() > MAX_EVENTS) {
                recent.removeLast();
            }
        }

        // 401·403 은 토큰 만료 같은 평범한 일로도 흔해서 WARN 으로 올리지 않는다.
        if (type == Type.UNAUTHORIZED || type == Type.FORBIDDEN) {
            log.debug("보안 이벤트 {} ip={} path={} {}", type, event.ip(), event.path(), event.detail());
        } else {
            log.warn("보안 이벤트 {} ip={} path={} {}", type, event.ip(), event.path(), event.detail());
        }
    }

    public synchronized Snapshot snapshot() {

        Map<Type, Long> all = new EnumMap<>(Type.class);

        for (Type type : Type.values()) {
            all.put(type, counts.getOrDefault(type, 0L));
        }

        return new Snapshot(since, Collections.unmodifiableMap(all), new ArrayList<>(recent));
    }

    /** 사용자가 보낸 값이 로그 줄을 바꾸거나 위조하지 못하게 제어 문자를 없애고 길이를 줄인다. */
    private static String clean(String value, int max) {

        if (value == null) {
            return "";
        }

        String cleaned = value.replaceAll("\\p{Cntrl}", " ");

        return cleaned.length() <= max ? cleaned : cleaned.substring(0, max) + "…";
    }
}
