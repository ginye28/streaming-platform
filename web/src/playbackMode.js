/**
 * 라이브 재생 방식(저지연 / 일반)과, 저지연을 못 쓸 때의 이유. 시청 화면의 표시와 서버 집계(관리자)가 같은 말을 쓴다.
 * 이유 값(UNSUPPORTED 등)은 서버 PlaybackStats.Reason 과 같아야 한다.
 */

/** lowLatency.js 가 알려 주는 실패 이름을 서버 이유로 바꾼다. */
export function reasonOf(failure) {
    switch (failure) {
        case 'codec':
            return 'CODEC'
        case 'source-buffer':
        case 'buffer-error':
        case 'append':
            return 'BUFFER'
        case 'timeout':
        case 'closed':
            return 'CONNECTION'
        case 'full':
            return 'FULL'
        case 'forbidden':
            return 'FORBIDDEN'
        case 'unavailable':
            return 'UNAVAILABLE'
        default:
            return 'OTHER'
    }
}

/** 저지연을 못 쓸 때 시청자에게 보여 주는 이유와 해결 방법. */
export const FALLBACK_COPY = {
    UNSUPPORTED: '이 브라우저는 저지연 재생을 지원하지 않아요. 컴퓨터의 Chrome·Edge 에서 열면 지연이 줄어요.',
    CODEC: '이 기기가 저지연 영상 형식을 재생하지 못해요.',
    BUFFER: '저지연 재생 중 오류가 반복돼 일반 재생으로 바꿨어요.',
    CONNECTION: '저지연 연결이 끊기거나 늦어서 일반 재생으로 바꿨어요. 네트워크가 안정되면 새로고침해 보세요.',
    FULL: '저지연으로 보는 시청자가 가득 차서 일반 재생으로 보고 있어요.',
    FORBIDDEN: '이 방송은 저지연으로 볼 수 없어서 일반 재생으로 보고 있어요.',
    UNAVAILABLE: '방송 중계가 아직 준비되지 않아 일반 재생으로 보고 있어요.',
    OTHER: '저지연 재생을 쓰지 못해 일반 재생으로 보고 있어요.',
}

/** 관리자 집계 표의 이유 이름. */
export const REASON_LABEL = {
    UNSUPPORTED: '브라우저 미지원',
    CODEC: '영상 형식 재생 불가',
    BUFFER: '재생 버퍼 오류',
    CONNECTION: '연결 끊김·지연',
    FULL: '저지연 자리 가득 참',
    FORBIDDEN: '저지연 시청 권한 없음',
    UNAVAILABLE: '중계 준비 안 됨',
    OTHER: '그 밖의 이유',
}
