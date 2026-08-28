import { useEffect, useRef, useState } from 'react'
import { getChatHistory, getLive, getLiveIntro } from '../api.js'
import { assetUrl } from '../assets.js'
import { useAuth } from '../useAuth.js'
import HlsPlayer from '../components/HlsPlayer.jsx'
import IntroGate from '../components/IntroGate.jsx'
import Link from '../components/Link.jsx'
import { useAsyncData } from '../useAsyncData.js'
import { useChat } from '../useChat.js'

export default function LivePage({ id }) {
    const { me } = useAuth()

    const { data: live, error, loading } = useAsyncData(() => getLive(id), [id])

    // 방송 정보와 나란히 받는다. 인트로 때문에 화면이 늦게 뜨면 안 된다.
    const { data: intro, loading: introLoading } = useAsyncData(
        () => getLiveIntro(id),
        [id]
    )

    // 지난 내역은 최신순으로 오므로 오래된 것부터 보이도록 뒤집는다.
    const { data: history } = useAsyncData(
        () => getChatHistory(id).then((page) => [...page.content].reverse()),
        [id]
    )

    // "어느 방송에서 들어가기를 눌렀는지" 를 들고 있는다.
    // 단순한 참/거짓으로 두면 이어보기로 넘어갔을 때 이전 판정이 남아 인트로를 건너뛴다.
    const [enteredLiveId, setEnteredLiveId] = useState(null)

    if (loading || introLoading) return <p className="empty">불러오는 중…</p>
    if (error) return <p className="error">{error}</p>
    if (!live) return null

    if (intro?.showGate && enteredLiveId !== id) {
        return <IntroGate intro={intro} liveId={id} onEnter={() => setEnteredLiveId(id)} />
    }

    return (
        <section className="live">
            <div className="live__main">
                <HlsPlayer src={live.hlsUrl} poster={live.thumbnailUrl} />

                <h2>{live.title}</h2>

                <p className="meta">
                    <Link to={{ view: 'channel', id: live.channelId }}>{live.nickname}</Link>
                    {live.status === 'ENDED' && ' · 종료된 방송'}
                </p>

                {live.description && <p className="description">{live.description}</p>}
            </div>

            {/*
              지난 내역을 받은 뒤에 연결해야 순서가 꼬이지 않는다.
              key 로 방송이 바뀌면 채팅 상태를 새로 시작한다.
            */}
            {history && (
                <ChatPanel key={id} liveId={id} history={history} canSend={Boolean(me)} />
            )}
        </section>
    )
}

function ChatPanel({ liveId, history, canSend }) {
    const { messages, viewerCount, connected, send } = useChat(liveId, history)
    const [content, setContent] = useState('')
    const listRef = useRef(null)

    // 새 메시지가 오면 맨 아래로 붙인다.
    useEffect(() => {
        const list = listRef.current

        if (list) {
            list.scrollTop = list.scrollHeight
        }
    }, [messages])

    function handleSubmit(event) {
        event.preventDefault()

        if (send(content.trim())) {
            setContent('')
        }
    }

    return (
        <aside className="chat">
            <div className="chat__header">
                채팅
                {viewerCount != null && <span className="meta"> · 시청자 {viewerCount}명</span>}
                {!connected && <span className="meta"> · 연결 중…</span>}
            </div>

            <ul className="chat__list" ref={listRef}>
                {messages.map((message) => (
                    <li key={message.id}>
                        {message.oshiMarkUrl && (
                            <img
                                className="chat__mark"
                                src={assetUrl(message.oshiMarkUrl)}
                                alt=""
                                title="이 채널 구독자"
                            />
                        )}
                        <strong>{message.nickname}</strong> {message.content}
                    </li>
                ))}
            </ul>

            {canSend ? (
                <form className="chat__form" onSubmit={handleSubmit}>
                    <input
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                        placeholder="메시지를 입력하세요"
                        maxLength={200}
                        required
                    />
                    <button type="submit" disabled={!connected}>
                        보내기
                    </button>
                </form>
            ) : (
                <p className="meta chat__form">채팅을 쓰려면 로그인이 필요합니다.</p>
            )}
        </aside>
    )
}
