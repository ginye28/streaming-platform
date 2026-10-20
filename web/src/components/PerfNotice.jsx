import { useEffect } from 'react'
import { dismissPerfNotice, setPerfMode, usePerf } from '../perf.js'

/**
 * 화면이 느려서 효과를 자동으로 줄였을 때 한 번만 알려 준다.
 * 숨기지 않고 이유를 말하고, 마음에 안 들면 바로 되돌릴 수 있게 한다.
 */
export default function PerfNotice() {
    const { autoDowngraded, reason } = usePerf()

    useEffect(() => {
        if (!autoDowngraded) return undefined

        const timer = setTimeout(dismissPerfNotice, 12_000)

        return () => clearTimeout(timer)
    }, [autoDowngraded])

    if (!autoDowngraded) return null

    return (
        <div className="perf-notice" role="status">
            <span>기기가 느려서 화면 효과를 줄였어요. ({reason})</span>
            <button type="button" onClick={() => setPerfMode('full')}>
                되돌리기
            </button>
            <button type="button" onClick={dismissPerfNotice} aria-label="알림 닫기">
                닫기
            </button>
        </div>
    )
}
