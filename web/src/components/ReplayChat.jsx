import { useEffect, useMemo, useRef, useState } from 'react'
import { getChatReplay } from '../api.js'
import { donationTier } from '../donation.js'
import { won } from '../payments.js'
import OshiMark from './OshiMark.jsx'

/** 한 번에 화면에 그리는 줄 수. 다시보기 채팅이 아주 길어도 화면이 무거워지지 않게 한다. */
const VISIBLE_LINES = 100

/** offsetSeconds 가 seconds 이하인 메시지가 몇 개인지(오래된 순으로 정렬돼 있다고 보고 이분 탐색한다). */
function countUpTo(messages, seconds) {
    let low = 0
    let high = messages.length

    while (low < high) {
        const mid = (low + high) >> 1

        if (messages[mid].offsetSeconds <= seconds) {
            low = mid + 1
        } else {
            high = mid
        }
    }

    return low
}

/**
 * 다시보기 채팅. 방송 때의 채팅을 영상 재생 위치에 맞춰 흘려 보낸다.
 *
 * 채팅은 오래된 것부터 500개씩 이어서 받아 온다. 방송이 길어도 처음 화면은 바로 뜨고, 뒤쪽은 받아 오는 동안
 * 채워진다. 아직 받지 못한 뒤쪽으로 건너뛰면 받아 오는 대로 따라잡는다.
 *
 * currentTime 은 영상의 재생 위치(초)다.
 */
export default function ReplayChat({ liveId, currentTime }) {
    const [messages, setMessages] = useState([])
    const [loaded, setLoaded] = useState(false)
    const [error, setError] = useState(null)
    const listRef = useRef(null)

    useEffect(() => {
        let cancelled = false

        async function loadAll() {
            let afterId

            while (!cancelled) {
                const page = await getChatReplay(liveId, afterId)

                if (cancelled) return

                setMessages((prev) => [...prev, ...page.messages])

                if (!page.nextAfterId) break

                afterId = page.nextAfterId
            }

            if (!cancelled) setLoaded(true)
        }

        loadAll().catch((e) => !cancelled && setError(e.message))

        return () => {
            cancelled = true
        }
    }, [liveId])

    const visible = useMemo(() => {
        const end = countUpTo(messages, currentTime)

        return messages.slice(Math.max(0, end - VISIBLE_LINES), end)
    }, [messages, currentTime])

    // 새 줄이 생기면 맨 아래로 붙인다.
    useEffect(() => {
        const list = listRef.current

        if (list) {
            list.scrollTop = list.scrollHeight
        }
    }, [visible])

    return (
        <aside className="chat">
            <div className="chat__header">
                다시보기 채팅
                {!loaded && !error && <span className="meta"> · 불러오는 중…</span>}
            </div>

            {error && <p className="chat__notice">{error}</p>}

            {loaded && messages.length === 0 && <p className="meta chat__form">남아 있는 채팅이 없습니다.</p>}

            <ul className="chat__list" ref={listRef}>
                {visible.map((message) => {
                    const donation = message.donationAmount != null

                    return (
                        <li
                            key={message.id}
                            className={
                                donation
                                    ? `chat-line chat-line--donation chat-line--t${donationTier(message.donationAmount)}`
                                    : 'chat-line'
                            }
                        >
                            {donation && <div className="chat-line__amount">{won(message.donationAmount)} 후원</div>}

                            <OshiMark url={message.oshiMarkUrl} tier={message.oshiTier} />

                            {message.role && (
                                <span className={`chat-badge chat-badge--${message.role.toLowerCase()}`}>
                                    {message.role === 'OWNER' ? '주인' : '매니저'}
                                </span>
                            )}

                            <strong>{message.nickname}</strong> {message.content}
                        </li>
                    )
                })}
            </ul>
        </aside>
    )
}
