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
 * 공개 서버(Render 등)에서는 RTMP_CALLBACK_TOKEN 을 채워 두면 nginx 가 주소에 붙여 보내는
 * token 이 맞을 때만 받는다. (재생 주소에 드러나는 방송 이름만으로 남의 방송을 끝내는 것을 막는다.)
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
    public ResponseEntity<Void> publish(
            @RequestParam("name") String name,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "hls", required = false) String hls
    ) {

        if (!liveProperties.callbackAllowed(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 스트리밍 서버가 자기 공개 주소를 알려 주면 시청자에게 그 주소를 내려 준다. (비밀 값이 있을 때만 받는다)
        if (hls != null && !liveProperties.acceptHlsOverride(hls)) {
            log.warn("스트리밍 서버가 알려 준 공개 주소를 받지 않았다");
        }

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
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "token", required = false) String token
    ) {

        if (!liveProperties.callbackAllowed(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        liveStreamService.endBroadcast(name);

        return ResponseEntity.ok().build();
    }
}
