package com.sp.api.live.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

@Getter
@Component
public class LiveProperties {

    private static final Pattern HLS_BASE_PATTERN =
            Pattern.compile("^https://[A-Za-z0-9.-]+(:[0-9]{1,5})?(/[A-Za-z0-9._~-]+)*$");

    /** 시청자에게 내려줄 HLS 주소의 앞부분. */
    @Value("${app.hls.base-url}")
    private String hlsBaseUrl;

    /**
     * 스트리밍 서버가 방송을 시작할 때 알려 준 공개 주소. 있으면 hlsBaseUrl 대신 쓴다.
     * 내 PC 에서 스트리밍 서버를 돌리고 임시 터널로 공개할 때, 터널 주소가 켤 때마다 바뀌어서 있는 기능이다.
     * 서버 메모리에만 있다(재시작하면 다음 방송 시작 때 다시 받는다).
     */
    private volatile String hlsOverride;

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

    /** 시청자에게 실제로 내려 줄 HLS 주소의 앞부분. */
    public String getHlsBaseUrl() {
        String override = hlsOverride;
        return override != null ? override : hlsBaseUrl;
    }

    /**
     * 스트리밍 서버가 알려 준 공개 주소를 받는다. 콜백 비밀 값이 설정돼 있고(= 호출자가 검증됐고),
     * https 주소처럼 생겼을 때만 받는다. 시청자 브라우저가 이 주소로 영상을 받으러 가므로 아무 값이나 받으면 안 된다.
     *
     * @return 받았으면 true
     */
    public boolean acceptHlsOverride(String candidate) {

        if (callbackToken == null || callbackToken.isBlank() || candidate == null) {
            return false;
        }

        String url = candidate.endsWith("/") ? candidate.substring(0, candidate.length() - 1) : candidate;

        if (!HLS_BASE_PATTERN.matcher(url).matches()) {
            return false;
        }

        this.hlsOverride = url;
        return true;
    }

    public String redirectUrlFor(String publicName) {
        return rtmpRedirectBase + "/" + publicName;
    }

    /** 방송(streamName)의 다시보기 재생 주소. */
    public String vodUrlFor(String streamName) {

        String base = vodBaseUrl == null || vodBaseUrl.isBlank()
                ? getHlsBaseUrl().replaceAll("/hls/?$", "/vod")
                : vodBaseUrl;

        return base + "/" + streamName + ".m3u8";
    }
}
