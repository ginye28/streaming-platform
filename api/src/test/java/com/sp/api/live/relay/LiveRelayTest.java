package com.sp.api.live.relay;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LiveRelayTest {

    private static final class Recorder implements LiveRelay.Subscriber {

        final List<String> events = new ArrayList<>();

        @Override
        public void onInit(String codecs, byte[] init) {
            events.add("init:" + codecs);
        }

        @Override
        public void onData(byte[] data) {
            events.add("data:" + new String(data));
        }

        @Override
        public void onEnd() {
            events.add("end");
        }
    }

    private static byte[] b(String value) {
        return value.getBytes();
    }

    @Test
    @DisplayName("늦게 들어온 시청자는 머리와 마지막 키프레임부터 지금까지를 받고, 그 뒤로 실시간을 받는다")
    void lateJoinerStartsFromLastKeyframe() {

        LiveRelay relay = new LiveRelay();
        FragmentListener feed = relay.open(1L);

        feed.onInit("avc1.640028", b("INIT"));
        feed.onFragment(b("k1"), true);
        feed.onFragment(b("p1"), false);
        feed.onFragment(b("k2"), true);
        feed.onFragment(b("p2"), false);

        Recorder viewer = new Recorder();
        relay.join(1L, viewer);

        feed.onFragment(b("p3"), false);

        assertThat(viewer.events).containsExactly("init:avc1.640028", "data:k2", "data:p2", "data:p3");
    }

    @Test
    @DisplayName("키프레임을 아직 못 본 상태의 중간 조각은 쌓지 않는다")
    void doesNotKeepFragmentsBeforeFirstKeyframe() {

        LiveRelay relay = new LiveRelay();
        FragmentListener feed = relay.open(2L);

        feed.onInit("avc1.42e01e", b("INIT"));
        feed.onFragment(b("p0"), false);

        Recorder viewer = new Recorder();
        relay.join(2L, viewer);

        assertThat(viewer.events).containsExactly("init:avc1.42e01e");
    }

    @Test
    @DisplayName("방송이 끝나면 시청자에게 알리고, 없는 방송에는 붙을 수 없다")
    void endNotifiesAndMissingChannelIsRefused() {

        LiveRelay relay = new LiveRelay();
        FragmentListener feed = relay.open(3L);
        feed.onInit("avc1.640028", b("INIT"));

        Recorder viewer = new Recorder();
        assertThat(relay.join(3L, viewer)).isNotNull();

        relay.close(3L);

        assertThat(viewer.events).last().isEqualTo("end");
        assertThat(relay.join(3L, new Recorder())).isNull();
        assertThat(relay.join(999L, new Recorder())).isNull();
    }

    @Test
    @DisplayName("떼어 낸 시청자에게는 더 이상 보내지 않는다")
    void leavingStopsDelivery() {

        LiveRelay relay = new LiveRelay();
        FragmentListener feed = relay.open(4L);
        feed.onInit("avc1.640028", b("INIT"));

        Recorder viewer = new Recorder();
        Runnable leave = relay.join(4L, viewer);

        feed.onFragment(b("k1"), true);
        leave.run();
        feed.onFragment(b("p1"), false);

        assertThat(viewer.events).containsExactly("init:avc1.640028", "data:k1");
        assertThat(relay.viewerCount(4L)).isZero();
    }

    @Test
    @DisplayName("조각을 보내는 도중 시청자가 스스로 떨어져 나가도(느린 시청자 끊기) 순회가 깨지지 않는다")
    void subscriberLeavingDuringDispatchIsSafe() {

        LiveRelay relay = new LiveRelay();
        FragmentListener feed = relay.open(6L);
        feed.onInit("avc1.640028", b("INIT"));

        Recorder stayer = new Recorder();
        Runnable[] leave = new Runnable[1];

        // 첫 조각을 받자마자 스스로 떨어지는 시청자(큐가 차서 서버가 끊는 상황과 같다)
        LiveRelay.Subscriber leaver = new LiveRelay.Subscriber() {
            @Override
            public void onInit(String codecs, byte[] init) {
            }

            @Override
            public void onData(byte[] data) {
                leave[0].run();
            }

            @Override
            public void onEnd() {
            }
        };

        leave[0] = relay.join(6L, leaver);
        relay.join(6L, stayer);

        feed.onFragment(b("k1"), true);
        feed.onFragment(b("p1"), false);

        assertThat(stayer.events).contains("data:k1", "data:p1");
        assertThat(relay.viewerCount(6L)).isEqualTo(1);
    }

    @Test
    @DisplayName("키프레임이 너무 오래 안 오면 쌓인 조각을 버려 메모리를 지킨다")
    void backlogIsBounded() {

        LiveRelay relay = new LiveRelay();
        FragmentListener feed = relay.open(5L);
        feed.onInit("avc1.640028", b("INIT"));

        feed.onFragment(new byte[1024], true);

        for (int i = 0; i < 8; i++) {
            feed.onFragment(new byte[1024 * 1024], false);
        }

        Recorder viewer = new Recorder();
        relay.join(5L, viewer);

        // 6MB 한도를 넘어서 비워졌으므로 머리만 받는다
        assertThat(viewer.events.size()).isLessThan(8);
    }
}
