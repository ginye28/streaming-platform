package com.sp.api.live.ingest;

import java.io.IOException;

public interface HlsPackagerFactory {

    /** streamName 방송의 HLS(streamName.m3u8, streamName-N.ts)를 만들기 시작한다. */
    HlsPackager open(String streamName) throws IOException;
}
