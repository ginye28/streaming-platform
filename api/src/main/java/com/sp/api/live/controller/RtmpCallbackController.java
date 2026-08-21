package com.sp.api.live.controller;

import com.sp.api.live.config.LiveProperties;
import com.sp.api.live.entity.LiveStream;
import com.sp.api.live.service.LiveStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * nginx-rtmp 의 on_publish / on_publish_done 콜백 수신부.
 *
 * JWT 가 아니라 스트림 키로 인증하므로 외부에 노출되면 안 된다.
 * 배포 시 /api/internal/** 는 반드시 내부망(또는 리버스 프록시 ACL)으로 제한할 것.
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/rtmp")
@RequiredArgsConstructor
public class RtmpCallbackController {

    private final LiveStreamService liveStreamService;
    private final LiveProperties liveProperties;

    /**
     * 2xx 면 송출 허용, 3xx 면 다른 이름으로 송출 전환, 그 외에는 nginx 가 연결을 끊는다.
     * name 파라미터에 OBS 의 "스트림 키" 값이 담겨 온다.
     *
     * 스트림 키로 그대로 송출하면 재생 URL(/hls/{name}.m3u8)에 키가 드러나므로,
     * 공개 이름으로 리다이렉트해 키를 감춘다.
     */
    @PostMapping("/publish")
    public ResponseEntity<Void> publish(@RequestParam("name") String name) {

        // 리다이렉트되어 공개 이름으로 다시 들어온 요청은 그대로 통과시킨다.
        if (liveProperties.isRenameOnPublish() && liveStreamService.isActiveRepublish(name)) {
            return ResponseEntity.ok().build();
        }

        LiveStream live = liveStreamService.startBroadcast(name);

        if (!liveProperties.isRenameOnPublish()) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(liveProperties.redirectUrlFor(live.getStreamName())))
                .build();
    }

    @PostMapping("/publish-done")
    public ResponseEntity<Void> publishDone(
            @RequestParam(value = "name", required = false) String name
    ) {

        liveStreamService.endBroadcast(name);

        return ResponseEntity.ok().build();
    }
}
