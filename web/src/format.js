/**
 * 숫자와 시간을 한국어 서비스에서 보는 모양으로.
 * 유튜브처럼 천·만 단위로 줄여 쓰고, 방송 시간은 말로 풀어 쓴다.
 */

/** 1234 → 1.2천, 12345 → 1.2만, 123456789 → 1.2억. 1000 미만은 그대로. */
export function compact(value) {
    const n = Math.max(0, Number(value) || 0)

    if (n < 1000) return String(n)
    if (n < 10_000) return `${trim(n / 1000)}천`
    if (n < 100_000_000) return `${n >= 1_000_000 ? Math.round(n / 10_000).toLocaleString('ko-KR') : trim(n / 10_000)}만`

    return `${trim(n / 100_000_000)}억`
}

function trim(x) {
    return x.toFixed(1).replace(/\.0$/, '')
}

/** 조회수: 3.2천회 */
export function formatViews(value) {
    return `조회수 ${compact(value)}회`
}

/** 시청자: 1.2천명 */
export function formatViewers(value) {
    return `${compact(value)}명`
}

/** 방송 시간: 막 시작했으면 "방금 시작", 아니면 "12분째" · "1시간 5분째". */
export function formatLiveFor(ms) {
    const minutes = Math.max(0, Math.floor(ms / 60000))

    if (minutes < 1) return '방금 시작'

    const hours = Math.floor(minutes / 60)
    const rest = minutes % 60

    if (hours === 0) return `${rest}분째`

    return rest === 0 ? `${hours}시간째` : `${hours}시간 ${rest}분째`
}

/** 영상 길이(초): 3분 45초 → "03:45", 1시간 2분 3초 → "1:02:03". 모르면 빈 문자열. */
export function formatDuration(seconds) {
    const total = Math.round(Number(seconds))

    if (!Number.isFinite(total) || total < 1) return ''

    const h = Math.floor(total / 3600)
    const m = Math.floor((total % 3600) / 60)
    const s = total % 60
    const two = (n) => String(n).padStart(2, '0')

    return h > 0 ? `${h}:${two(m)}:${two(s)}` : `${two(m)}:${two(s)}`
}
