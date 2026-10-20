import { useEffect, useState } from 'react'
import { serverTimeMs } from './time.js'

/** 지금 시각. 방송 시간이 흘러가 보이도록 1분마다 새로 잰다. */
export function useNow(intervalMs = 60_000) {
    const [now, setNow] = useState(() => Date.now())

    useEffect(() => {
        const timer = setInterval(() => setNow(Date.now()), intervalMs)
        return () => clearInterval(timer)
    }, [intervalMs])

    return now
}

/** 서버 시각(시간대 표시 없는 UTC)을 밀리초로. 읽을 수 없으면 fallback. */
export function toTime(value, fallback = Date.now()) {
    return serverTimeMs(value, fallback)
}
