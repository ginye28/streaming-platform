import { Client } from '@stomp/stompjs'
import { useEffect, useRef, useState } from 'react'
import { API_BASE_URL, getAccessToken } from './api.js'

const BROKER_URL = `${API_BASE_URL.replace(/^http/, 'ws')}/ws`

/**
 * 라이브 채팅. /topic/lives/{liveId} 를 구독해 메시지를,
 * /topic/lives/{liveId}/viewers 로 시청자 수를 받는다.
 *
 * 시청자 수는 채팅방 구독 수로 세므로, 이 훅이 붙어 있는 동안만 집계에 포함된다.
 *
 * initialMessages 는 첫 렌더에만 쓰인다. 방송이 바뀌면 호출하는 쪽에서
 * key 로 컴포넌트를 다시 마운트해야 한다.
 */
export function useChat(liveId, initialMessages = []) {
    const [messages, setMessages] = useState(initialMessages)
    const [viewerCount, setViewerCount] = useState(null)
    const [connected, setConnected] = useState(false)
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
                    const message = JSON.parse(frame.body)
                    setMessages((prev) => [...prev, message])
                })

                client.subscribe(`/topic/lives/${liveId}/viewers`, (frame) => {
                    setViewerCount(JSON.parse(frame.body).viewerCount)
                })
            },

            onWebSocketClose: () => setConnected(false),
        })

        client.activate()
        clientRef.current = client

        return () => {
            clientRef.current = null
            client.deactivate()
        }
    }, [liveId])

    function send(content) {
        const client = clientRef.current

        if (!client?.connected) return false

        client.publish({
            destination: `/app/lives/${liveId}/chat`,
            body: JSON.stringify({ content }),
        })

        return true
    }

    return { messages, viewerCount, connected, send }
}
