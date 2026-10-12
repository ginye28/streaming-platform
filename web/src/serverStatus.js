import { useSyncExternalStore } from 'react'

/**
 * 서버(무료 Render)가 잠들었다 깨는 동안의 상태.
 *
 * 무료 서버는 한동안 접속이 없으면 잠들고, 깨는 데 최대 3분쯤 걸린다. 그동안 요청은 연결 오류나
 * 502/503/504 로 끝난다. 화면마다 빨간 오류를 늘어놓는 대신, 한 곳에서 "깨우는 중" 을 알리고
 * 깨어나면 알려 준다.
 *
 *   ok      평소
 *   waking  요청이 서버에 닿지 않았다. 몇 초마다 다시 확인 중
 *   ready   깨어났다. 화면의 오류를 지우려면 새로고침이 필요하다
 */
let status = 'ok'
let timer = null
const listeners = new Set()

function set(next) {
    if (status === next) return

    status = next
    listeners.forEach((listener) => listener())
}

/** 요청이 서버에 닿지 못했을 때 부른다. probe 는 서버가 살았는지 true/false 로 알려 주는 함수. */
export function reportUnreachable(probe) {
    if (status !== 'ok') return

    set('waking')

    const check = async () => {
        let alive = false

        try {
            alive = await probe()
        } catch {
            // 아직 안 깬 것
        }

        if (alive) {
            clearInterval(timer)
            timer = null
            set('ready')
        }
    }

    timer = setInterval(check, 4000)
    check()
}

/** "새로고침해도 사용자가 잃을 것이 없는 화면"에서만 깨어나면 저절로 새로고침한다. */
export function dismissReady() {
    set('ok')
}

export function useServerStatus() {
    return useSyncExternalStore(
        (listener) => {
            listeners.add(listener)
            return () => listeners.delete(listener)
        },
        () => status
    )
}
