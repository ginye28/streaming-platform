import { useState } from 'react'
import {
    getChannel,
    getChannelLive,
    getChannelProfile,
    getChannelSchedules,
    getChannelLiveHistory,
    getChannelStreams,
    toggleBlock,
} from '../api.js'
import { assetUrl } from '../assets.js'
import { useAuth } from '../useAuth.js'
import ChannelIdentity from '../components/ChannelIdentity.jsx'
import Pager from '../components/Pager.jsx'
import SubscribeBar from '../components/SubscribeBar.jsx'
import ScheduleList from '../components/ScheduleList.jsx'
import StreamList from '../components/StreamList.jsx'
import Link from '../components/Link.jsx'
import { timeAgo } from '../time.js'
import { useAsyncData } from '../useAsyncData.js'
import { usePageTitle } from '../usePageTitle.js'
import { useSlow } from '../useSlow.js'

export default function ChannelPage({ id }) {
    const { me } = useAuth()
    const [pageNumber, setPageNumber] = useState(0)
    // 차단은 서버가 토글이라 처음 상태를 알 수 없다. 이 화면에서 바꾼 결과만 안다.
    const [blocked, setBlocked] = useState(false)
    const [panel, setPanel] = useState(false)
    const [notice, setNotice] = useState(null)
    const [actionError, setActionError] = useState(null)

    const { data: channel, error, loading, reload } = useAsyncData(() => getChannel(id), [id])

    const { data: live } = useAsyncData(() => getChannelLive(id), [id])

    const { data: profile } = useAsyncData(() => getChannelProfile(id), [id])

    const {
        data: streams,
        error: streamsError,
        loading: streamsLoading,
        reload: reloadStreams,
    } = useAsyncData(() => getChannelStreams(id, pageNumber), [id, pageNumber])

    const { data: history } = useAsyncData(() => getChannelLiveHistory(id, 0), [id])

    const { data: schedules } = useAsyncData(() => getChannelSchedules(id), [id])

    usePageTitle(channel?.nickname)

    // 무료 서버가 잠들어 있으면 첫 응답이 오래 걸린다. 그 이유를 알려 준다.
    const slow = useSlow(loading)

    function failWith(e) {
        setActionError(typeof e === 'string' ? e : (e?.message ?? '처리하지 못했어요.'))
    }

    async function handleBlock() {
        setActionError(null)

        try {
            const result = await toggleBlock(id)
            setBlocked(result.blocked)
            setPanel(false)
            setNotice(result.blocked ? '차단했어요. 이 채널의 방송과 영상이 목록에서 보이지 않아요.' : '차단을 해제했어요.')
        } catch (e) {
            setPanel(false)
            failWith(e)
        }
    }

    if (loading) {
        return (
            <p className="empty" role="status">
                채널을 불러오는 중…
                {slow && (
                    <>
                        <br />
                        서버를 깨우는 중이에요. 최대 1분쯤 걸려요
                    </>
                )}
            </p>
        )
    }

    // 채널 정보 자체를 못 받았을 때만 페이지 전체를 오류로 바꾼다.
    if (error && !channel) {
        return (
            <p className="error" role="alert">
                {error} <button type="button" onClick={reload}>다시 시도</button>
            </p>
        )
    }

    if (!channel) return null

    const mine = me?.id === channel.id

    return (
        <section className="channel">
            <header className="channel__head">
                <span className={`channel__avatar${channel.live ? ' channel__avatar--live' : ''}`} aria-hidden="true">
                    {channel.profileImage ? (
                        <img src={assetUrl(channel.profileImage)} alt="" />
                    ) : (
                        channel.nickname.slice(0, 1)
                    )}
                </span>

                <div className="channel__who">
                    <h1>
                        {channel.nickname}
                        {channel.live && <span className="pill pill--live">LIVE</span>}
                    </h1>

                    <p className="meta">
                        {profile?.fanName ?? '구독자'} {channel.subscriberCount.toLocaleString('ko-KR')}명 · 영상{' '}
                        {channel.streamCount.toLocaleString('ko-KR')}개
                    </p>
                </div>
            </header>

            <ChannelIdentity profile={profile} />

            <SubscribeBar channel={channel} onChanged={reload} onFail={failWith} />

            {me && !mine && (
                <div className="channel__manage">
                    <button
                        type="button"
                        onClick={() => (blocked ? handleBlock() : setPanel(true))}
                        aria-expanded={panel}
                    >
                        {blocked ? '차단 해제' : '차단'}
                    </button>
                </div>
            )}

            {panel && (
                <div className="stream__panel" role="group" aria-label="채널 차단 확인">
                    <p>이 채널을 차단할까요? 방송과 영상이 목록에서 보이지 않게 돼요. 언제든 해제할 수 있어요.</p>
                    <div className="stream__panel-actions">
                        <button type="button" className="button--danger" onClick={handleBlock}>
                            차단
                        </button>
                        <button type="button" onClick={() => setPanel(false)}>
                            취소
                        </button>
                    </div>
                </div>
            )}

            {notice && (
                <p className="meta" role="status">
                    {notice}
                </p>
            )}

            {actionError && (
                <p className="error" role="alert">
                    {actionError}{' '}
                    <button type="button" onClick={() => setActionError(null)}>
                        닫기
                    </button>
                </p>
            )}

            {live && (
                <Link to={{ view: 'live', id: live.id }} className="live-card">
                    <span className="live-card__thumb">
                        {live.thumbnailUrl && <img src={assetUrl(live.thumbnailUrl)} alt="" />}
                        <span className="pill pill--live">LIVE</span>
                    </span>

                    <span className="live-card__text">
                        <strong className="live-card__title">{live.title}</strong>
                        <span className="live-card__meta">
                            지금 방송 중 · 시청자 {live.viewerCount?.toLocaleString('ko-KR')}명
                        </span>
                    </span>
                </Link>
            )}

            {schedules?.length > 0 && (
                <>
                    <h2>방송 예정</h2>
                    <ScheduleList schedules={schedules} showChannel={false} />
                </>
            )}

            <h2>영상</h2>

            {streamsError && (
                <p className="error" role="alert">
                    {streamsError} <button type="button" onClick={reloadStreams}>다시 시도</button>
                </p>
            )}

            {streamsLoading && !streams ? (
                <p className="empty" role="status">
                    영상을 불러오는 중…
                </p>
            ) : (
                !streamsError && (
                    <StreamList
                        streams={streams?.content}
                        showChannel={false}
                        empty="아직 올라온 영상이 없어요. 구독해 두면 새 영상이 올라올 때 알려 드려요."
                    />
                )
            )}

            <Pager page={streams} onChange={setPageNumber} />

            {history?.content.length > 0 && (
                <>
                    <h2>지난 방송</h2>
                    <ul className="past">
                        {history.content.map((past) => (
                            <li key={past.id}>
                                <PastBroadcast past={past} />
                            </li>
                        ))}
                    </ul>
                </>
            )}
        </section>
    )
}

/** 지난 방송 한 줄. 다시볼 수 있거나 구독자 전용일 때만 링크하고, 다시보기가 없는 방송은 글자로만 둔다. */
function PastBroadcast({ past }) {
    const replay = Boolean(past.vodUrl)
    const linkable = replay || past.locked
    const when = timeAgo(past.endedAt)

    const content = (
        <>
            <span className="past__title">{past.title}</span>
            <span className="past__meta">
                {when && `${when} · `}
                {past.locked ? (
                    <span className="past__chip">구독자 전용</span>
                ) : replay ? (
                    <span className="past__chip past__chip--on">다시보기</span>
                ) : (
                    '다시보기 없음'
                )}
            </span>
        </>
    )

    return linkable ? (
        <Link to={{ view: 'live', id: past.id }} className="past__link">
            {content}
        </Link>
    ) : (
        <div className="past__link past__link--off">{content}</div>
    )
}
