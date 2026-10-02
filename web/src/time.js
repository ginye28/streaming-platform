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
