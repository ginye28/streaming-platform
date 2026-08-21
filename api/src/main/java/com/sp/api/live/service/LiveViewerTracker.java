package com.sp.api.live.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 방송별 동시 시청자 수를 메모리에서 센다.
 *
 * 채팅 WebSocket 연결을 기준으로 하므로 정확한 시청자 수는 아니고,
 * 서버를 여러 대로 늘리면 인스턴스별로 따로 세어진다.
 * 정확한 집계가 필요해지면 Redis 로 옮겨야 한다.
 */
@Component
public class LiveViewerTracker {

    private final Map<Long, Set<String>> viewersByLiveId = new ConcurrentHashMap<>();

    /** @return 갱신된 시청자 수 */
    public long join(Long liveId, String sessionId) {

        Set<String> viewers =
                viewersByLiveId.computeIfAbsent(liveId, key -> ConcurrentHashMap.newKeySet());

        viewers.add(sessionId);

        return viewers.size();
    }

    /** @return 갱신된 시청자 수 */
    public long leave(Long liveId, String sessionId) {

        Set<String> viewers = viewersByLiveId.get(liveId);

        if (viewers == null) {
            return 0;
        }

        viewers.remove(sessionId);

        if (viewers.isEmpty()) {
            viewersByLiveId.remove(liveId);
            return 0;
        }

        return viewers.size();
    }

    public long count(Long liveId) {

        Set<String> viewers = viewersByLiveId.get(liveId);

        return viewers == null ? 0 : viewers.size();
    }

    public void clear(Long liveId) {
        viewersByLiveId.remove(liveId);
    }
}
