package com.sp.api.live.ingest;

import com.sp.api.live.relay.FragmentListener;

import java.io.IOException;

public interface HlsPackagerFactory {

    /** streamName 방송의 HLS(streamName.m3u8, streamName-N.ts)를 만들기 시작한다. */
    HlsPackager open(String streamName) throws IOException;

    /**
     * HLS 와 함께, 같은 입력에서 짧은 fMP4 조각을 만들어 listener 에 넘긴다(저지연 시청용).
     * listener 가 null 이면 HLS 만 만든다. 기본 구현은 fMP4 를 만들지 않는다(테스트의 가짜 작업기).
     */
    default HlsPackager open(String streamName, FragmentListener listener) throws IOException {
        return open(streamName);
    }
}
