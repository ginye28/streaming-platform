package com.sp.api.live.relay;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 ffmpeg 가 만든 조각난 MP4(키프레임 0.5초 간격, 영상+소리) 로 분리기를 확인한다. */
class Fmp4SplitterTest {

    private static final Path SAMPLE = Path.of("src/test/resources/fixtures/sample-fragmented.mp4");

    private static class Collector implements FragmentListener {

        String codecs;
        byte[] init;
        final List<byte[]> fragments = new ArrayList<>();
        final List<Boolean> keyframes = new ArrayList<>();

        @Override
        public void onInit(String codecs, byte[] init) {
            this.codecs = codecs;
            this.init = init;
        }

        @Override
        public void onFragment(byte[] fragment, boolean keyframe) {
            fragments.add(fragment);
            keyframes.add(keyframe);
        }
    }

    @Test
    @DisplayName("머리와 0.1초 단위 조각으로 나누고, 코덱 문자열과 키프레임을 알아낸다")
    void splitsInitAndFragments() throws Exception {

        byte[] sample = Files.readAllBytes(SAMPLE);
        Collector collector = new Collector();
        Fmp4Splitter splitter = new Fmp4Splitter(collector);

        splitter.feed(sample, 0, sample.length);

        assertThat(splitter.isFailed()).isFalse();
        assertThat(collector.init).isNotNull();
        assertThat(collector.codecs).startsWith("avc1.").endsWith(",mp4a.40.2");

        // 2초 분량을 0.1초 단위로 → 대략 20개 이상. 키프레임은 0.5초마다라 4개 안팎.
        assertThat(collector.fragments.size()).isGreaterThanOrEqualTo(15);

        long keyframes = collector.keyframes.stream().filter(k -> k).count();

        assertThat(keyframes).isBetween(3L, 6L);

        // 첫 조각은 키프레임으로 시작해야 늦게 들어온 시청자가 거기서부터 받을 수 있다.
        assertThat(collector.keyframes.get(0)).isTrue();
    }

    @Test
    @DisplayName("머리 + 조각을 이어 붙이면 원본(끝의 mfra 제외)과 같다 — 바이트를 잃지 않는다")
    void doesNotLoseBytes() throws Exception {

        byte[] sample = Files.readAllBytes(SAMPLE);
        Collector collector = new Collector();

        new Fmp4Splitter(collector).feed(sample, 0, sample.length);

        ByteArrayOutputStream joined = new ByteArrayOutputStream();
        joined.write(collector.init);

        for (byte[] fragment : collector.fragments) {
            joined.write(fragment);
        }

        byte[] result = joined.toByteArray();

        assertThat(result.length).isLessThanOrEqualTo(sample.length);
        assertThat(sample).startsWith(result);
    }

    @Test
    @DisplayName("임의의 크기로 잘라 들어와도 같은 결과가 나온다(네트워크 읽기 경계)")
    void sameResultForArbitraryChunking() throws Exception {

        byte[] sample = Files.readAllBytes(SAMPLE);

        Collector whole = new Collector();
        new Fmp4Splitter(whole).feed(sample, 0, sample.length);

        Collector pieces = new Collector();
        Fmp4Splitter splitter = new Fmp4Splitter(pieces);
        Random random = new Random(42);

        int position = 0;

        while (position < sample.length) {
            int size = Math.min(1 + random.nextInt(900), sample.length - position);
            splitter.feed(sample, position, size);
            position += size;
        }

        assertThat(pieces.codecs).isEqualTo(whole.codecs);
        assertThat(pieces.init).isEqualTo(whole.init);
        assertThat(pieces.fragments).hasSameSizeAs(whole.fragments);
        assertThat(pieces.keyframes).isEqualTo(whole.keyframes);

        for (int i = 0; i < whole.fragments.size(); i++) {
            assertThat(pieces.fragments.get(i)).isEqualTo(whole.fragments.get(i));
        }
    }

    @Test
    @DisplayName("깨진 크기의 박스가 오면 멈추고 실패로 표시한다(메모리를 부풀리지 않는다)")
    void stopsOnCorruptedStream() {

        Collector collector = new Collector();
        Fmp4Splitter splitter = new Fmp4Splitter(collector);

        byte[] junk = {0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 'm', 'o', 'o', 'f', 1, 2, 3, 4};

        splitter.feed(junk, 0, junk.length);

        assertThat(splitter.isFailed()).isTrue();
        assertThat(collector.fragments).isEmpty();
    }
}
