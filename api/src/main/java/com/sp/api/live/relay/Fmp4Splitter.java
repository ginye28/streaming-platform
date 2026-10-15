package com.sp.api.live.relay;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ffmpeg 가 파이프로 내보내는 조각난 MP4(fMP4)를 읽어, 머리(ftyp+moov)와 조각(moof+mdat)으로 나눈다.
 *
 * 조각이 영상 키프레임으로 시작하는지도 알아낸다(moof 안 trun/tfhd 의 샘플 플래그). 시청자가 중간에 들어오면
 * 마지막 키프레임부터 받아야 화면이 깨지지 않기 때문이다. 박스 구조는 단순해서 직접 읽는다(외부 라이브러리 없음).
 *
 * 한 줄기 스트림 전용이고 스레드 안전하지 않다(ffmpeg 출력을 읽는 스레드 하나가 쓴다).
 */
public class Fmp4Splitter {

    /** 박스 하나의 최대 크기. 이보다 크면 깨진 스트림으로 보고 멈춘다(메모리를 보호한다). */
    private static final int MAX_BOX_BYTES = 16 * 1024 * 1024;

    private final FragmentListener listener;

    private byte[] buffer = new byte[256 * 1024];
    private int length;

    private final ByteArrayOutputStream head = new ByteArrayOutputStream();
    private boolean initSent;
    private long videoTrackId = -1;

    private byte[] pendingMoof;
    private boolean failed;

    public Fmp4Splitter(FragmentListener listener) {
        this.listener = listener;
    }

    /** 스트림이 깨져서 더 읽지 않는다. */
    public boolean isFailed() {
        return failed;
    }

    public void feed(byte[] data, int offset, int count) {

        if (failed) {
            return;
        }

        if (length + count > buffer.length) {
            byte[] grown = new byte[Math.max(buffer.length * 2, length + count)];
            System.arraycopy(buffer, 0, grown, 0, length);
            buffer = grown;
        }

        System.arraycopy(data, offset, buffer, length, count);
        length += count;

        int position = 0;

        while (length - position >= 8) {

            long size = ((buffer[position] & 0xFFL) << 24) | ((buffer[position + 1] & 0xFFL) << 16)
                    | ((buffer[position + 2] & 0xFFL) << 8) | (buffer[position + 3] & 0xFFL);

            if (size < 8 || size > MAX_BOX_BYTES) {
                // 크기 필드가 0(끝까지)·1(64비트)이거나 비정상. ffmpeg 가 이런 박스를 내는 설정이 아니다.
                failed = true;
                length = 0;
                return;
            }

            if (length - position < size) {
                break;
            }

            String type = new String(buffer, position + 4, 4, StandardCharsets.ISO_8859_1);
            byte[] box = new byte[(int) size];
            System.arraycopy(buffer, position, box, 0, (int) size);

            handle(type, box);

            position += (int) size;
        }

        if (position > 0) {
            System.arraycopy(buffer, position, buffer, 0, length - position);
            length -= position;
        }
    }

    private void handle(String type, byte[] box) {

        switch (type) {
            case "ftyp" -> head.write(box, 0, box.length);

            case "moov" -> {
                head.write(box, 0, box.length);

                if (!initSent) {
                    initSent = true;
                    videoTrackId = findVideoTrackId(box);
                    listener.onInit(codecsOf(box), head.toByteArray());
                }
            }

            case "moof" -> pendingMoof = box;

            case "mdat" -> {
                if (pendingMoof != null && initSent) {
                    byte[] fragment = new byte[pendingMoof.length + box.length];
                    System.arraycopy(pendingMoof, 0, fragment, 0, pendingMoof.length);
                    System.arraycopy(box, 0, fragment, pendingMoof.length, box.length);

                    listener.onFragment(fragment, startsWithKeyframe(pendingMoof));
                }

                pendingMoof = null;
            }

            default -> {
                // mfra·free·sidx 등. 중계에는 필요 없다.
            }
        }
    }

    // ── 코덱 문자열 ──

    /** moov 안의 avcC·mp4a 를 찾아 "avc1.PPCCLL[,mp4a.40.2]" 를 만든다. */
    static String codecsOf(byte[] moov) {

        List<String> codecs = new ArrayList<>();

        int avcC = indexOf(moov, "avcC");

        if (avcC >= 0 && avcC + 8 <= moov.length) {
            // avcC 4글자 다음: configurationVersion, profile, compatibility, level
            codecs.add(String.format(Locale.ROOT, "avc1.%02x%02x%02x",
                    moov[avcC + 5] & 0xFF, moov[avcC + 6] & 0xFF, moov[avcC + 7] & 0xFF));
        }

        if (indexOf(moov, "mp4a") >= 0) {
            codecs.add("mp4a.40.2");
        }

        return String.join(",", codecs);
    }

    private static int indexOf(byte[] data, String ascii) {

        byte[] needle = ascii.getBytes(StandardCharsets.ISO_8859_1);

        outer:
        for (int i = 0; i <= data.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }

        return -1;
    }

    // ── 박스 읽기 ──

    /** 이 부모 박스의 자식 박스들의 (유형, 시작 위치, 크기). start~end 는 부모의 내용 범위. */
    private static List<int[]> children(byte[] data, int start, int end, List<String> types) {

        List<int[]> result = new ArrayList<>();
        int position = start;

        while (position + 8 <= end) {

            int size = ByteBuffer.wrap(data, position, 4).getInt();

            if (size < 8 || position + size > end) {
                break;
            }

            types.add(new String(data, position + 4, 4, StandardCharsets.ISO_8859_1));
            result.add(new int[]{position, size});

            position += size;
        }

        return result;
    }

    /** moov → trak → (tkhd, mdia → hdlr) 에서 'vide' 트랙의 id. 못 찾으면 -1. */
    private static long findVideoTrackId(byte[] moov) {

        List<String> types = new ArrayList<>();
        List<int[]> boxes = children(moov, 8, moov.length, types);

        for (int i = 0; i < boxes.size(); i++) {

            if (!"trak".equals(types.get(i))) {
                continue;
            }

            int trakStart = boxes.get(i)[0] + 8;
            int trakEnd = boxes.get(i)[0] + boxes.get(i)[1];

            long trackId = -1;
            boolean video = false;

            List<String> trakTypes = new ArrayList<>();

            List<int[]> trakChildren = children(moov, trakStart, trakEnd, trakTypes);

            for (int j = 0; j < trakChildren.size(); j++) {

                int at = trakChildren.get(j)[0];

                if ("tkhd".equals(trakTypes.get(j))) {
                    int version = moov[at + 8] & 0xFF;
                    int idOffset = at + 8 + 4 + (version == 1 ? 16 : 8);
                    trackId = ByteBuffer.wrap(moov, idOffset, 4).getInt() & 0xFFFFFFFFL;

                } else if ("mdia".equals(trakTypes.get(j))) {

                    List<String> mdiaTypes = new ArrayList<>();
                    List<int[]> mdia = children(moov, at + 8, at + trakChildren.get(j)[1], mdiaTypes);

                    for (int k = 0; k < mdia.size(); k++) {
                        if ("hdlr".equals(mdiaTypes.get(k))) {
                            int handler = mdia.get(k)[0] + 8 + 4 + 4;
                            video = "vide".equals(new String(moov, handler, 4, StandardCharsets.ISO_8859_1));
                        }
                    }
                }
            }

            if (video) {
                return trackId;
            }
        }

        return -1;
    }

    /** moof 안에 영상 트랙의 첫 샘플이 키프레임(동기 샘플)인 traf 가 있으면 true. */
    boolean startsWithKeyframe(byte[] moof) {

        List<String> types = new ArrayList<>();
        List<int[]> boxes = children(moof, 8, moof.length, types);

        for (int i = 0; i < boxes.size(); i++) {

            if (!"traf".equals(types.get(i))) {
                continue;
            }

            int start = boxes.get(i)[0] + 8;
            int end = boxes.get(i)[0] + boxes.get(i)[1];

            List<String> trafTypes = new ArrayList<>();
            List<int[]> trafChildren = children(moof, start, end, trafTypes);

            long trackId = -1;
            Long defaultFlags = null;

            for (int j = 0; j < trafChildren.size(); j++) {

                int at = trafChildren.get(j)[0];

                if ("tfhd".equals(trafTypes.get(j))) {

                    int flags = ByteBuffer.wrap(moof, at + 8, 4).getInt() & 0x00FFFFFF;
                    int p = at + 12;

                    trackId = ByteBuffer.wrap(moof, p, 4).getInt() & 0xFFFFFFFFL;
                    p += 4;

                    if ((flags & 0x01) != 0) p += 8;   // base_data_offset
                    if ((flags & 0x02) != 0) p += 4;   // sample_description_index
                    if ((flags & 0x08) != 0) p += 4;   // default_sample_duration
                    if ((flags & 0x10) != 0) p += 4;   // default_sample_size
                    if ((flags & 0x20) != 0) {
                        defaultFlags = ByteBuffer.wrap(moof, p, 4).getInt() & 0xFFFFFFFFL;
                    }

                } else if ("trun".equals(trafTypes.get(j))) {

                    if (videoTrackId != -1 && trackId != videoTrackId) {
                        continue;
                    }

                    Long first = firstSampleFlags(moof, at, defaultFlags);

                    if (first != null && (first & 0x10000L) == 0) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static Long firstSampleFlags(byte[] moof, int trunAt, Long defaultFlags) {

        int flags = ByteBuffer.wrap(moof, trunAt + 8, 4).getInt() & 0x00FFFFFF;
        int p = trunAt + 12;

        int sampleCount = ByteBuffer.wrap(moof, p, 4).getInt();
        p += 4;

        if (sampleCount <= 0) {
            return null;
        }

        if ((flags & 0x001) != 0) p += 4;   // data_offset

        if ((flags & 0x004) != 0) {
            return ByteBuffer.wrap(moof, p, 4).getInt() & 0xFFFFFFFFL;
        }

        if ((flags & 0x400) != 0) {
            // 첫 샘플의 항목: [duration][size] 다음이 flags
            if ((flags & 0x100) != 0) p += 4;
            if ((flags & 0x200) != 0) p += 4;
            return ByteBuffer.wrap(moof, p, 4).getInt() & 0xFFFFFFFFL;
        }

        return defaultFlags;
    }
}
