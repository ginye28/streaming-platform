/** 서버가 주는 시간대 붙은 시각("2026-10-05T20:00:00+09:00")을 이 브라우저의 시간대로 보기 좋게. */
export function formatWhen(iso) {
    if (!iso) return ''

    return new Date(iso).toLocaleString('ko-KR', {
        month: 'long',
        day: 'numeric',
        weekday: 'short',
        hour: '2-digit',
        minute: '2-digit',
    })
}

/** 서버 시각 문자열을 <input type="datetime-local"> 이 받는 모양("2026-10-05T20:00")으로. 이 브라우저의 시간대 기준이다. */
export function toLocalInput(iso) {
    const date = new Date(iso)
    const pad = (value) => String(value).padStart(2, '0')

    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/**
 * <input type="datetime-local"> 의 값("2026-10-05T20:00")을 시간대가 붙은 시각으로.
 * 시간대 없이 보내면 서버가 자기 시간대로 읽어 어긋나므로, 이 브라우저의 시간대를 붙여 보낸다.
 */
export function fromLocalInput(value) {
    return new Date(value).toISOString()
}

/** 지금부터 target 까지 남은 시간을 { days, hours, minutes, seconds, done } 으로. */
export function countdown(target, now = Date.now()) {
    const left = Math.max(0, new Date(target).getTime() - now)
    const seconds = Math.floor(left / 1000)

    return {
        days: Math.floor(seconds / 86400),
        hours: Math.floor((seconds % 86400) / 3600),
        minutes: Math.floor((seconds % 3600) / 60),
        seconds: seconds % 60,
        done: left === 0,
    }
}

/** 지나간 시간(ms)을 "3분 전", "어제" 처럼. */
export function formatAgo(ms) {
    const minutes = Math.floor(ms / 60000)
    if (minutes < 60) return `${Math.max(1, minutes)}분 전`
    const hours = Math.floor(minutes / 60)
    if (hours < 24) return `${hours}시간 전`
    const days = Math.floor(hours / 24)
    if (days < 7) return days === 1 ? '어제' : `${days}일 전`
    if (days < 30) return `${Math.floor(days / 7)}주 전`
    return `${Math.floor(days / 30)}달 전`
}

/**
 * 서버가 주는 시간대 없는 시각("2026-10-05T21:14:00")을 "3일 전" 처럼.
 * 서버는 시간대 없는 LocalDateTime 을 주므로 이 브라우저와 같은 시간대라고 보고 읽는다.
 */
export function timeAgo(value) {
    if (!value) return ''

    const time = new Date(value).getTime()

    return Number.isNaN(time) ? '' : formatAgo(Date.now() - time)
}

/** "2025-03-12" 같은 날짜(시간 없음)를 "2025년 3월 12일" 로. 시간대에 따라 하루가 밀리지 않게 직접 쪼갠다. */
export function formatDay(value) {
    const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value ?? '')

    return match ? `${match[1]}년 ${Number(match[2])}월 ${Number(match[3])}일` : (value ?? '')
}
