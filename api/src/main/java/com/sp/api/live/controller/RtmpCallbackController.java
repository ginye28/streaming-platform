package com.sp.api.live.controller;

import com.sp.api.live.service.LiveStreamService;
import com.sp.api.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * 2xx 를 돌려주면 송출 허용, 그 외에는 nginx 가 연결을 끊는다.
     * name 파라미터에 OBS 의 "스트림 키" 값이 담겨 온다.
     */
    @PostMapping("/publish")
    public ResponseEntity<Void> publish(@RequestParam("name") String streamKey) {

        User user = liveStreamService.authorizePublish(streamKey);

        log.info("송출 시작: userId={}", user.getId());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/publish-done")
    public ResponseEntity<Void> publishDone(@RequestParam(value = "name", required = false) String streamKey) {

        log.info("송출 종료");

        return ResponseEntity.ok().build();
    }
}
