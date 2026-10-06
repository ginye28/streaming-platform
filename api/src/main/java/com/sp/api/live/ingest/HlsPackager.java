package com.sp.api.live.ingest;

import java.io.IOException;

/**
 * 브라우저가 보낸 WebM 조각을 받아 HLS 로 만들어 내는 한 방송분의 작업기.
 * 실제로는 ffmpeg 프로세스이고, 테스트에서는 바이트만 세는 가짜로 바꾼다.
 */
public interface HlsPackager {

    /** 브라우저가 보낸 조각을 순서대로 넣는다. 작업기가 죽었으면 IOException. */
    void write(byte[] chunk) throws IOException;

    /** 입력을 닫고 정리한다. 여러 번 불러도 된다. */
    void close();
}
