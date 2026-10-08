import { useEffect, useState } from 'react'

/**
 * active 가 delay 이상 이어지면 true.
 * 무료 서버가 잠들어 첫 응답이 오래 걸릴 때, 그 이유를 알려 줄 시점을 잡는 데 쓴다.
 */
export function useSlow(active, delay = 5000) {
    const [slow, setSlow] = useState(false)

    useEffect(() => {
        if (!active) return

        const timer = setTimeout(() => setSlow(true), delay)
        return () => {
            clearTimeout(timer)
            setSlow(false)
        }
    }, [active, delay])

    return slow
}
