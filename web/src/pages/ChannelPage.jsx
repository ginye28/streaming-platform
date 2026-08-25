import { useState } from 'react'
import {
    getChannel,
    getChannelLive,
    getChannelLiveHistory,
    getChannelStreams,
    toggleBlock,
    toggleSubscribe,
} from '../api.js'
import { useAuth } from '../useAuth.js'
import Pager from '../components/Pager.jsx'
import StreamList from '../components/StreamList.jsx'
import Link from '../components/Link.jsx'
import { useAsyncData } from '../useAsyncData.js'

export default function ChannelPage({ id }) {
    const { me } = useAuth()
    const [pageNumber, setPageNumber] = useState(0)

    const {
        data: channel,
        error,
        loading,
        reload,
        fail,
    } = useAsyncData(() => getChannel(id), [id])

    const { data: live } = useAsyncData(() => getChannelLive(id), [id])

    const { data: streams } = useAsyncData(
        () => getChannelStreams(id, pageNumber),
        [id, pageNumber]
    )

    const { data: history } = useAsyncData(() => getChannelLiveHistory(id, 0), [id])

    async function handleSubscribe() {
        try {
            await toggleSubscribe(id)
            reload()
        } catch (e) {
            fail(e)
        }
    }

    async function handleBlock() {
        if (!window.confirm('이 채널을 차단할까요? 목록에서 보이지 않게 됩니다.')) return

        try {
            const { blocked } = await toggleBlock(id)
            window.alert(blocked ? '차단했습니다.' : '차단을 해제했습니다.')
        } catch (e) {
            fail(e)
        }
    }

    if (loading) return <p className="empty">불러오는 중…</p>
    if (error) return <p className="error">{error}</p>
    if (!channel) return null

    const mine = me?.id === channel.id

    return (
        <section>
            <h2>{channel.nickname}</h2>

            <p className="meta">
                구독자 {channel.subscriberCount}명 · 영상 {channel.streamCount}개
                {channel.live && ' · 방송 중'}
            </p>

            {me && !mine && (
                <div className="toolbar">
                    <button onClick={handleSubscribe}>
                        {channel.subscribedByMe ? '구독 중' : '구독'}
                    </button>
                    <button onClick={handleBlock}>차단</button>
                </div>
            )}

            {live && (
                <p>
                    <Link to={{ view: 'live', id: live.id }}>
                        지금 방송 중 — {live.title} (시청자 {live.viewerCount}명)
                    </Link>
                </p>
            )}

            <h3>영상</h3>
            <StreamList streams={streams?.content} />
            <Pager page={streams} onChange={setPageNumber} />

            {history?.content.length > 0 && (
                <>
                    <h3>지난 방송</h3>
                    <ul>
                        {history.content.map((past) => (
                            <li key={past.id}>
                                <Link to={{ view: 'live', id: past.id }}>{past.title}</Link>
                                <span className="meta">
                                    {' '}
                                    · 최고 시청자 {past.peakViewerCount}명
                                </span>
                            </li>
                        ))}
                    </ul>
                </>
            )}
        </section>
    )
}
