import { useEffect, useState } from 'react'
import { usePerf } from '../perf.js'

/**
 * 기기에서 직접 프레임을 보려고 켜는 작은 계기판. 주소에 ?fps=1 을 붙이면 나타난다(?fps=0 으로 끈다).
 * 지난 1초의 평균 fps 와 가장 느렸던 프레임 간격을 보여 준다. 실제 기기 테스트 체크리스트: docs/10-performance.md.
 */
export default function FpsMeter() {
    const perf = usePerf()
    const [stats, setStats] = useState({ fps: 0, worst: 0 })

    useEffect(() => {
        let raf = 0
        let last = performance.now()
        let windowStart = last
        let frames = 0
        let worst = 0

        const tick = (now) => {
            frames += 1
            worst = Math.max(worst, now - last)
            last = now

            if (now - windowStart >= 1000) {
                setStats({ fps: Math.round((frames * 1000) / (now - windowStart)), worst: Math.round(worst) })
                windowStart = now
                frames = 0
                worst = 0
            }

            raf = requestAnimationFrame(tick)
        }

        raf = requestAnimationFrame(tick)

        return () => cancelAnimationFrame(raf)
    }, [])

    const cores = navigator.hardwareConcurrency ?? '?'
    const memory = navigator.deviceMemory ? `${navigator.deviceMemory}GB` : '?GB'

    return (
        <div className="c-fps" aria-hidden="true">
            <b>{stats.fps} fps</b> · 최악 {stats.worst}ms
            <br />
            {perf.level} ({perf.mode}) · {cores}코어 · {memory} · dpr {window.devicePixelRatio}
        </div>
    )
}
