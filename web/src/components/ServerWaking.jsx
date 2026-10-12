import { useEffect } from 'react'
import { dismissReady, useServerStatus } from '../serverStatus.js'
import { useRoute } from '../router.js'

/** 깨어나면 저절로 새로고침해도 되는 화면. 입력 중이거나 방송 중인 화면은 사용자가 직접 누르게 둔다. */
const SAFE_TO_RELOAD = new Set(['home', 'lives', 'live', 'stream', 'channel', 'search', 'subscribed', 'schedule'])

/**
 * 무료 서버가 잠들었다 깨는 동안(최대 3분) 화면 맨 위에 안내를 띄운다.
 * 화면마다 빨간 오류가 따로 뜨는 대신 "기다리면 된다" 는 것을 한 곳에서 알려 주고, 깨어나면 새로고침한다.
 */
export default function ServerWaking() {
    const status = useServerStatus()
    const { view } = useRoute()

    const autoReload = status === 'ready' && SAFE_TO_RELOAD.has(view)

    useEffect(() => {
        if (!autoReload) return

        const timer = setTimeout(() => window.location.reload(), 1200)

        return () => clearTimeout(timer)
    }, [autoReload])

    if (status === 'ok') return null

    if (status === 'waking') {
        return (
            <div className="server-waking" role="status" aria-live="polite">
                <strong>서버를 깨우는 중이에요.</strong> 무료 서버라 한동안 안 쓰면 잠들고, 깨는 데 최대 3분쯤 걸려요.
                끝나면 자동으로 이어집니다. 이 화면을 닫지 마세요.
            </div>
        )
    }

    return (
        <div className="server-waking server-waking--ready" role="status" aria-live="polite">
            <strong>서버가 깨어났어요.</strong>{' '}
            {autoReload ? '화면을 새로 불러옵니다…' : '하던 일을 마친 뒤 새로고침하면 최신 내용이 보여요.'}
            <button type="button" onClick={() => window.location.reload()}>
                지금 새로고침
            </button>
            {!autoReload && (
                <button type="button" onClick={dismissReady}>
                    닫기
                </button>
            )}
        </div>
    )
}
