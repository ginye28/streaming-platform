import { Client } from '@stomp/stompjs'
import { useEffect, useRef, useState } from 'react'
import { API_BASE_URL, getAccessToken } from './api.js'

const BROKER_URL = `${API_BASE_URL.replace(/^http/, 'ws')}/ws`

/** 화면에 남겨 두는 메시지 수. 오래 켜 둔 방송에서 목록이 끝없이 늘지 않게 한다. */
const MAX_MESSAGES = 500

/**
 * 서버가 준 메시지에 "후원 띠가 언제까지 보이는지" 를 붙인다.
 * 서버는 남은 초(donationPinRemainingSeconds)만 주고, 시각은 이 브라우저의 시계로 센다 —
 * 서버 시각과 브라우저 시간대가 달라도 어긋나지 않게 하려는 것이다.
 */
export function withPinExpiry(message) {
    if (message.donationAmount == null || message.donationPinRemainingSeconds == null) {
        return message
    }

    return { ...message, pinExpiresAt: Date.now() + message.donationPinRemainingSeconds * 1000 }
}

/**
 * 라이브 채팅.
 * - /topic/lives/{id}          메시지(일반 채팅과 후원)
 * - /topic/lives/{id}/events   삭제·고정·슬로우 모드 같은 방 안의 변화
 * - /topic/lives/{id}/viewers  시청자 수
 * - /user/queue/chat-errors    내가 보낸 채팅이 거절된 이유(나에게만 온다)
 *
 * 시청자 수는 채팅방 구독 수로 세므로, 이 훅이 붙어 있는 동안만 집계에 포함된다.
 *
 * initial 은 첫 렌더에만 쓰인다. 방송이 바뀌면 호출하는 쪽에서 key 로 컴포넌트를 다시 마운트해야 한다.
 */
export function useChat(liveId, initial = {}) {
    const [messages, setMessages] = useState(() => (initial.messages ?? []).map(withPinExpiry))
    const [pinned, setPinned] = useState(initial.pinned ?? null)
    const [slowModeSeconds, setSlowModeSeconds] = useState(initial.slowModeSeconds ?? 0)
    const [chatAudience, setChatAudience] = useState(initial.chatAudience ?? 'ALL')
    const [viewerCount, setViewerCount] = useState(null)
    const [connected, setConnected] = useState(false)
    const [notice, setNotice] = useState(null)
    const clientRef = useRef(null)

    useEffect(() => {
        if (!liveId) return

        const token = getAccessToken()

        const client = new Client({
            brokerURL: BROKER_URL,
            // 비로그인도 연결은 되고, 읽기만 가능하다.
            connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
            reconnectDelay: 3000,

            onConnect: () => {
                setConnected(true)

                client.subscribe(`/topic/lives/${liveId}`, (frame) => {
                    const message = withPinExpiry(JSON.parse(frame.body))

                    setMessages((prev) => [...prev, message].slice(-MAX_MESSAGES))
                })

                client.subscribe(`/topic/lives/${liveId}/events`, (frame) => {
                    const event = JSON.parse(frame.body)

                    switch (event.type) {
                        case 'DELETE':
                            setMessages((prev) => prev.filter((m) => m.id !== event.messageId))
                            setPinned((prev) => (prev?.id === event.messageId ? null : prev))
                            break
                        case 'PURGE':
                            setMessages((prev) => prev.filter((m) => m.userId !== event.userId))
                            setPinned((prev) => (prev?.userId === event.userId ? null : prev))
                            break
                        case 'PIN':
                            setPinned(event.message ?? null)
                            break
                        case 'SETTINGS':
                            setSlowModeSeconds(event.slowModeSeconds ?? 0)
                            setChatAudience(event.chatAudience ?? 'ALL')
                            break
                        default:
                    }
                })

                client.subscribe(`/topic/lives/${liveId}/viewers`, (frame) => {
                    setViewerCount(JSON.parse(frame.body).viewerCount)
                })

                client.subscribe('/user/queue/chat-errors', (frame) => {
                    setNotice(JSON.parse(frame.body).message)
                })
            },

            // 서버가 구독을 거절하면(구독자 전용 방송) 이 프레임이 온다.
            onStompError: (frame) => setNotice(frame.headers?.message ?? '채팅에 연결하지 못했습니다.'),

            onWebSocketClose: () => setConnected(false),
        })

        client.activate()
        clientRef.current = client

        return () => {
            clientRef.current = null
            client.deactivate()
        }
    }, [liveId])

    // 알림은 몇 초 뒤에 저절로 사라진다.
    useEffect(() => {
        if (!notice) return

        const timer = setTimeout(() => setNotice(null), 5000)

        return () => clearTimeout(timer)
    }, [notice])

    function send(content) {
        const client = clientRef.current

        if (!client?.connected) return false

        client.publish({
            destination: `/app/lives/${liveId}/chat`,
            body: JSON.stringify({ content }),
        })

        return true
    }

    return {
        messages,
        pinned,
        slowModeSeconds,
        chatAudience,
        viewerCount,
        connected,
        notice,
        send,
    }
}
