package com.sp.api.live.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Getter
@Component
public class LiveProperties {

    /** 시청자에게 내려줄 HLS 주소의 앞부분. */
    @Value("${app.hls.base-url}")
    private String hlsBaseUrl;

    /**
     * on_publish 응답으로 공개 이름으로의 리다이렉트를 돌려줄지 여부.
     * 끄면 재생 URL 에 송출 키가 그대로 드러난다.
     */
    @Value("${app.rtmp.rename-on-publish}")
    private boolean renameOnPublish;

    /** 리다이렉트할 RTMP 주소의 앞부분. nginx 가 인식하는 주소여야 한다. */
    @Value("${app.rtmp.redirect-base}")
    private String rtmpRedirectBase;

    /**
     * 스트리밍 서버가 방송을 다시보기로 남기는지(streaming/nginx.conf 의 vod 애플리케이션).
     * 켜면 방송이 끝날 때 다시보기 주소를 내려 준다. 녹화를 안 하는 서버에서 켜면 없는 주소를 가리키게 된다.
     */
    @Value("${app.vod.enabled:false}")
    private boolean vodEnabled;

    /** 다시보기 재생 주소의 앞부분. 기본은 HLS 주소 옆의 /vod. */
    @Value("${app.vod.base-url:}")
    private String vodBaseUrl;

    /**
     * nginx-rtmp 콜백(/api/internal/rtmp/**)이 주소에 붙여 보내는 비밀 값.
     * 비어 있으면 검사하지 않는다(내부망에서만 닿는 로컬 환경). 공개 서버에서는 반드시 채운다.
     */
    @Value("${app.rtmp.callback-token:}")
    private String callbackToken;

    public boolean callbackAllowed(String token) {

        if (callbackToken == null || callbackToken.isBlank()) {
            return true;
        }

        return token != null && MessageDigest.isEqual(
                callbackToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    public String redirectUrlFor(String publicName) {
        return rtmpRedirectBase + "/" + publicName;
    }

    /** 방송(streamName)의 다시보기 재생 주소. */
    public String vodUrlFor(String streamName) {

        String base = vodBaseUrl == null || vodBaseUrl.isBlank()
                ? hlsBaseUrl.replaceAll("/hls/?$", "/vod")
                : vodBaseUrl;

        return base + "/" + streamName + ".m3u8";
    }
}
