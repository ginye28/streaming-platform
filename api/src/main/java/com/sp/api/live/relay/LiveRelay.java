package com.sp.api.live.relay;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 브라우저 방송을 HLS 조각을 거치지 않고 시청자에게 바로 흘려 보내는 중계 허브.
 *
 * 방송마다 채널 하나를 두고, ffmpeg 가 내보낸 짧은 fMP4 조각을 받는 즉시 구독한 시청자에게 넘긴다.
 * 시청자는 MSE 로 재생한다. HLS 는 조각 하나(약 3.4초)가 다 만들어져야 받을 수 있지만 이쪽은 0.1초 단위라
 * 지연이 1~2초로 줄어든다.
 *
 * 늦게 들어온 시청자에게는 머리(init)와 "마지막 키프레임부터 지금까지"를 먼저 준다.
 * 메모리에만 두며(서버 한 대 가정), 채널당 backlog 는 {@link #MAX_BACKLOG_BYTES} 로 막는다.
 */
@Slf4j
@Component
public class LiveRelay {

    /** 키프레임 이후로 쌓아 두는 최대 양. 1.5Mbps 에서 키프레임 간격 3.4초면 약 0.7MB 라 넉넉하다. */
    static final int MAX_BACKLOG_BYTES = 6 * 1024 * 1024;

    /** 시청자 쪽 수신기. 반드시 막히지 않게(큐에 넣기만) 구현해야 한다 — 중계 스레드에서 불린다. */
    public interface Subscriber {

        void onInit(String codecs, byte[] init);

        void onData(byte[] data);

        /** 방송이 끝났다. */
        void onEnd();
    }

    private final Map<Long, Channel> channels = new ConcurrentHashMap<>();

    /** 방송이 시작될 때 채널을 열고, ffmpeg 출력을 받을 수신기를 돌려준다. */
    public FragmentListener open(Long liveId) {

        Channel channel = new Channel(liveId);
        Channel previous = channels.put(liveId, channel);

        if (previous != null) {
            previous.end();
        }

        return channel;
    }

    /** 방송이 끝나면 채널을 닫고 시청자에게 알린다. */
    public void close(Long liveId) {

        Channel channel = channels.remove(liveId);

        if (channel != null) {
            channel.end();
        }
    }

    /**
     * 시청자를 붙인다. 아직 머리가 오지 않았으면 오는 대로 준다.
     *
     * @return 떼어 낼 때 쓰는 손잡이. 방송 채널이 없으면 null
     */
    public Runnable join(Long liveId, Subscriber subscriber) {

        Channel channel = channels.get(liveId);

        if (channel == null) {
            return null;
        }

        channel.add(subscriber);

        return () -> channel.remove(subscriber);
    }

    public int viewerCount(Long liveId) {
        Channel channel = channels.get(liveId);
        return channel == null ? 0 : channel.subscribers.size();
    }

    private static final class Channel implements FragmentListener {

        private final Long liveId;
        // 조각을 보내는 도중에 느린 시청자가 스스로 떨어져 나가므로(remove), 순회 중 수정에 안전해야 한다.
        private final List<Subscriber> subscribers = new CopyOnWriteArrayList<>();

        private String codecs;
        private byte[] init;

        private final Deque<byte[]> backlog = new ArrayDeque<>();
        private long backlogBytes;
        private boolean ended;

        Channel(Long liveId) {
            this.liveId = liveId;
        }

        @Override
        public synchronized void onInit(String codecs, byte[] init) {

            this.codecs = codecs;
            this.init = init;

            for (Subscriber subscriber : subscribers) {
                subscriber.onInit(codecs, init);
            }
        }

        @Override
        public synchronized void onFragment(byte[] fragment, boolean keyframe) {

            if (ended) {
                return;
            }

            if (keyframe) {
                backlog.clear();
                backlogBytes = 0;
            }

            // 키프레임을 한 번도 못 봤으면(backlog 가 비어 있으면) 중간부터 시작하는 조각은 쌓지 않는다.
            if (!backlog.isEmpty() || keyframe) {
                backlog.addLast(fragment);
                backlogBytes += fragment.length;

                if (backlogBytes > MAX_BACKLOG_BYTES) {
                    // 키프레임이 오래 안 오는 이상한 스트림. 다음 키프레임까지 쌓지 않는다.
                    backlog.clear();
                    backlogBytes = 0;
                    log.warn("방송 {} 의 키프레임 간격이 너무 길어 지난 조각을 버렸습니다.", liveId);
                }
            }

            for (Subscriber subscriber : subscribers) {
                subscriber.onData(fragment);
            }
        }

        synchronized void add(Subscriber subscriber) {

            if (ended) {
                subscriber.onEnd();
                return;
            }

            // 머리와 지난 조각을 먼저, 그다음부터 실시간. 같은 잠금 안이라 순서가 섞이지 않는다.
            if (init != null) {
                subscriber.onInit(codecs, init);

                for (byte[] fragment : backlog) {
                    subscriber.onData(fragment);
                }
            }

            subscribers.add(subscriber);
        }

        synchronized void remove(Subscriber subscriber) {
            subscribers.remove(subscriber);
        }

        synchronized void end() {

            if (ended) {
                return;
            }

            ended = true;

            for (Subscriber subscriber : subscribers) {
                subscriber.onEnd();
            }

            subscribers.clear();
            backlog.clear();
        }
    }
}
