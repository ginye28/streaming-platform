package com.sp.api.live.relay;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class PlaybackStatsTest {

    private final PlaybackStats stats = new PlaybackStats(Clock.fixed(Instant.parse("2026-11-02T00:00:00Z"), ZoneOffset.UTC));

    @Test
    @DisplayName("서버가 센 값과 화면이 보고한 값을 따로 모으고, HLS 는 이유별로 센다")
    void countsServerAndReportedValues() {

        stats.recordJoin();
        stats.recordJoin();
        stats.recordSlowDisconnect();
        stats.recordFullRejected();

        stats.report(PlaybackStats.Mode.LOW_LATENCY, null);
        stats.report(PlaybackStats.Mode.HLS, PlaybackStats.Reason.CODEC);
        stats.report(PlaybackStats.Mode.HLS, PlaybackStats.Reason.CODEC);
        stats.report(PlaybackStats.Mode.HLS, null);

        PlaybackStats.Snapshot snapshot = stats.snapshot(3);

        assertThat(snapshot.since()).isEqualTo(Instant.parse("2026-11-02T00:00:00Z"));
        assertThat(snapshot.lowLatencyViewersNow()).isEqualTo(3);
        assertThat(snapshot.lowLatencyJoined()).isEqualTo(2);
        assertThat(snapshot.slowDisconnects()).isEqualTo(1);
        assertThat(snapshot.fullRejected()).isEqualTo(1);
        assertThat(snapshot.reportedLowLatency()).isEqualTo(1);
        assertThat(snapshot.reportedHls()).isEqualTo(3);
        assertThat(snapshot.hlsReasons().get(PlaybackStats.Reason.CODEC)).isEqualTo(2);
        assertThat(snapshot.hlsReasons().get(PlaybackStats.Reason.OTHER)).isEqualTo(1);
        assertThat(snapshot.hlsReasons().get(PlaybackStats.Reason.UNSUPPORTED)).isZero();
    }
}
