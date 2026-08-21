package com.sp.api.live.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    public String redirectUrlFor(String publicName) {
        return rtmpRedirectBase + "/" + publicName;
    }
}
