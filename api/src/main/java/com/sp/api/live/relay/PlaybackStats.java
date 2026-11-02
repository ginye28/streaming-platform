package com.sp.api.live.relay;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 저지연 시청이 잘 되는지 숫자로 보는 곳. 관리자가 "시청자가 저지연으로 보고 있나, HLS 로 밀려났나"를 볼 수 있게 한다.
 *
 * 두 군데에서 센다.
 *  - 서버: 저지연 연결이 붙은 횟수, 느려서 끊은 횟수, 자리가 없어 거절한 횟수
 *  - 시청자 화면: 저지연으로 재생을 시작했는지, 일반(HLS) 재생으로 넘어갔다면 이유가 무엇인지(보고 API)
 *
 * 메모리에만 둔다(서버를 재시작하면 0 부터). 감사 기록이 아니라 지금 상태를 보는 창이다(SecurityEventRecorder 와 같은 성격).
 */
@Component
public class PlaybackStats {

    /** 화면이 보고하는 재생 방식. */
    public enum Mode {
        /** 저지연(WebSocket)으로 재생을 시작했다 */
        LOW_LATENCY,
        /** 저지연을 못 써서 일반(HLS)으로 재생한다 */
        HLS
    }

    /** 저지연을 쓰지 못한 이유. 화면이 보내는 값은 이 목록 안에서만 받는다. */
    public enum Reason {
        /** 이 브라우저가 저지연 재생(MediaSource)을 지원하지 않는다 */
        UNSUPPORTED,
        /** 이 기기가 영상 형식을 못 푼다 */
        CODEC,
        /** 재생 버퍼 오류가 반복됐다 */
        BUFFER,
        /** 연결이 끊기거나 시간 안에 영상이 오지 않았다 */
        CONNECTION,
        /** 저지연 시청자 자리가 가득 찼다 */
        FULL,
        /** 이 방송은 저지연으로 볼 수 없는 시청자다(구독자 전용 등) */
        FORBIDDEN,
        /** 방송 중계가 아직 준비되지 않았다 */
        UNAVAILABLE,
        /** 그 밖의 이유 */
        OTHER
    }

    public record Snapshot(
            Instant since,
            int lowLatencyViewersNow,
            long lowLatencyJoined,
            long slowDisconnects,
            long fullRejected,
            long reportedLowLatency,
            long reportedHls,
            Map<Reason, Long> hlsReasons
    ) {
    }

    private final Instant since;
    private final AtomicLong joined = new AtomicLong();
    private final AtomicLong slowDisconnects = new AtomicLong();
    private final AtomicLong fullRejected = new AtomicLong();
    private final AtomicLong reportedLowLatency = new AtomicLong();
    private final AtomicLong reportedHls = new AtomicLong();
    private final Map<Reason, AtomicLong> reasons = new EnumMap<>(Reason.class);

    public PlaybackStats(Clock clock) {
        this.since = clock.instant();

        for (Reason reason : Reason.values()) {
            reasons.put(reason, new AtomicLong());
        }
    }

    public void recordJoin() {
        joined.incrementAndGet();
    }

    public void recordSlowDisconnect() {
        slowDisconnects.incrementAndGet();
    }

    public void recordFullRejected() {
        fullRejected.incrementAndGet();
    }

    /** 화면이 알려 준 재생 방식. HLS 면 이유도 센다(이유가 없으면 OTHER). */
    public void report(Mode mode, Reason reason) {

        if (mode == Mode.LOW_LATENCY) {
            reportedLowLatency.incrementAndGet();
            return;
        }

        reportedHls.incrementAndGet();
        reasons.get(reason == null ? Reason.OTHER : reason).incrementAndGet();
    }

    /** @param viewersNow 지금 저지연으로 붙어 있는 시청자 수(LiveRelay 가 센다) */
    public Snapshot snapshot(int viewersNow) {

        Map<Reason, Long> counts = new EnumMap<>(Reason.class);
        reasons.forEach((reason, count) -> counts.put(reason, count.get()));

        return new Snapshot(
                since,
                viewersNow,
                joined.get(),
                slowDisconnects.get(),
                fullRejected.get(),
                reportedLowLatency.get(),
                reportedHls.get(),
                counts
        );
    }
}
