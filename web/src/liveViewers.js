import { useEffect, useState } from 'react'
import { API_BASE_URL } from './api.js'

/**
 * 목록 화면(홈·라이브)에서 방송 카드의 시청자 수를 실시간으로 받는다.
 *
 * 카드마다 연결을 만들지 않고 WebSocket 하나를 모두가 나눠 쓴다. 방송마다 /topic/lives/{id}/viewers 를 구독하고,
 * 구독하는 카드가 없어지면 연결을 닫는다. 탭이 가려져 있는 동안에도 닫는다(보이지 않는 화면에 서버 일을 시키지 않는다).
 *
 * 이 구독은 시청자 수 집계에 들어가지 않는다 — 집계는 채팅방(/topic/lives/{id}) 구독만 센다(LiveViewerEventListener).
 * stomp 는 홈을 열 때 같이 내려오지 않도록 필요할 때 불러온다.
 */

const BROKER_URL = `${API_BASE_URL.replace(/^http/, 'ws')}/ws`

/** 마지막 구독자가 사라진 뒤에도 잠깐 연결을 두어, 화면을 오갈 때 다시 맺지 않게 한다. */
const IDLE_CLOSE_MS = 10_000

const listeners = new Map() // liveId -> Set<(count) => void>
const topicSubs = new Map() // liveId -> STOMP 구독
const latest = new Map() // liveId -> 마지막으로 받은 시청자 수

let client = null
let loading = false
let idleTimer = 0
let refreshing = false

/** 한 번에 현재 값을 확인하는 방송 수의 상한. 카드가 많아도 요청이 몰리지 않게 한다. */
const MAX_REFRESH = 20

/**
 * 지금 시청자 수를 한 번 받아 온다. 밀어 주는 값은 "바뀔 때"만 오기 때문에,
 * 탭을 가렸다 다시 보거나 연결이 끊겼다 돌아오면 그 사이에 바뀐 값을 놓친다. 그걸 메운다.
 */
async function refresh() {
    if (refreshing || listeners.size === 0) return

    refreshing = true

    try {
        const ids = [...listeners.keys()].slice(0, MAX_REFRESH)

        await Promise.all(
            ids.map(async (liveId) => {
                try {
                    const response = await fetch(`${API_BASE_URL}/api/lives/${liveId}`)
                    if (!response.ok) return

                    const { data } = await response.json()
                    if (typeof data?.viewerCount !== 'number') return

                    latest.set(liveId, data.viewerCount)
                    listeners.get(liveId)?.forEach((notify) => notify(data.viewerCount))
                } catch {
                    // 확인에 실패해도 밀려 오는 값이 있으니 조용히 넘어간다.
                }
            })
        )
    } finally {
        refreshing = false
    }
}

function subscribeTopic(liveId) {
    if (!client?.connected || topicSubs.has(liveId)) return

    topicSubs.set(
        liveId,
        client.subscribe(`/topic/lives/${liveId}/viewers`, (frame) => {
            const { viewerCount } = JSON.parse(frame.body)

            latest.set(liveId, viewerCount)
            listeners.get(liveId)?.forEach((notify) => notify(viewerCount))
        })
    )
}

async function connect() {
    if (client || loading || document.hidden || listeners.size === 0) return

    loading = true

    try {
        const { Client } = await import('@stomp/stompjs')

        // 불러오는 사이에 구독자가 모두 사라졌거나 탭이 가려졌으면 만들지 않는다.
        if (document.hidden || listeners.size === 0) return

        client = new Client({
            brokerURL: BROKER_URL,
            reconnectDelay: 5000,
            onConnect: () => {
                topicSubs.clear()
                listeners.forEach((_, liveId) => subscribeTopic(liveId))
                refresh()
            },
        })

        client.activate()
    } finally {
        loading = false
    }
}

function disconnect() {
    clearTimeout(idleTimer)

    if (client) {
        client.deactivate()
        client = null
    }

    topicSubs.clear()
}

function watch(liveId, notify) {
    clearTimeout(idleTimer)

    if (!listeners.has(liveId)) listeners.set(liveId, new Set())
    listeners.get(liveId).add(notify)

    if (client) subscribeTopic(liveId)
    else connect()

    return () => {
        const set = listeners.get(liveId)
        set?.delete(notify)

        if (set && set.size === 0) {
            listeners.delete(liveId)
            topicSubs.get(liveId)?.unsubscribe()
            topicSubs.delete(liveId)
        }

        if (listeners.size === 0) {
            idleTimer = setTimeout(disconnect, IDLE_CLOSE_MS)
        }
    }
}

if (typeof document !== 'undefined') {
    document.addEventListener('visibilitychange', () => {
        if (document.hidden) disconnect()
        else connect()
    })
}

/**
 * 이 방송의 지금 시청자 수. 서버가 바뀔 때마다 밀어 주는 값을 쓰고, 아직 받은 게 없으면 목록이 준 값(initial)을 쓴다.
 */
export function useLiveViewerCount(liveId, initial) {
    const [pushed, setPushed] = useState(() => latest.get(liveId) ?? null)

    useEffect(() => {
        if (!liveId) return undefined

        return watch(liveId, setPushed)
    }, [liveId])

    return pushed ?? initial ?? 0
}
