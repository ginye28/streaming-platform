import { useState } from 'react'
import { getChatHistory, getLive, getLiveIntro } from '../api.js'
import { loginTo } from '../router.js'
import { usePageTitle } from '../usePageTitle.js'
import { useAuth } from '../useAuth.js'
import ChannelSubscribe from '../components/ChannelSubscribe.jsx'
import ChatPanel from '../components/ChatPanel.jsx'
import HlsPlayer from '../components/HlsPlayer.jsx'
import IntroGate from '../components/IntroGate.jsx'
import Link from '../components/Link.jsx'
import ReplayChat from '../components/ReplayChat.jsx'
import { useAsyncData } from '../useAsyncData.js'

const LOCK_COPY = {
    SUBSCRIBERS: {
        title: '구독자 전용 방송',
        body: '이 채널을 구독하면 볼 수 있어요.',
    },
    PAID: {
        title: '유료 구독자 전용 방송',
        body: '이 채널을 유료로 구독하면 볼 수 있어요.',
    },
}

export default function LivePage({ id }) {
    const { me } = useAuth()

    const { data: live, error, loading, reload } = useAsyncData(() => getLive(id), [id])

    usePageTitle(live?.title)

    // 방송 정보와 나란히 받는다. 인트로 때문에 화면이 늦게 뜨면 안 된다.
    const { data: intro, loading: introLoading } = useAsyncData(
        () => getLiveIntro(id),
        [id]
    )

    // 지난 내역은 최신순으로 오므로 오래된 것부터 보이도록 뒤집고, 지운 메시지는 뺀다.
    // 볼 수 없는 방송이면 서버가 거절하는데, 그때는 채팅창을 그리지 않으니 오류를 따로 보이지 않는다.
    const { data: history } = useAsyncData(
        () =>
            getChatHistory(id)
                .then((page) => [...page.content].reverse().filter((message) => !message.deleted))
                .catch(() => []),
        [id]
    )

    // "어느 방송에서 들어가기를 눌렀는지" 를 들고 있는다.
    // 단순한 참/거짓으로 두면 이어보기로 넘어갔을 때 이전 판정이 남아 인트로를 건너뛴다.
    const [enteredLiveId, setEnteredLiveId] = useState(null)

    if (loading || introLoading) return <p className="empty">불러오는 중…</p>
    if (error) return <p className="error">{error}</p>
    if (!live) return null

    if (live.locked) {
        return <LockedLive live={live} onChanged={reload} />
    }

    if (live.status === 'ENDED') {
        return <VodView live={live} />
    }

    if (intro?.showGate && enteredLiveId !== id) {
        return <IntroGate intro={intro} liveId={id} onEnter={() => setEnteredLiveId(id)} />
    }

    return (
        <section className="live">
            <div className="live__main">
                <HlsPlayer
                    src={live.hlsUrl}
                    poster={live.thumbnailUrl}
                    label={`${live.title} 라이브 방송`}
                    waitingLabel="방송 준비 중"
                >
                    {/* 송출이 오기 전에는 LIVE 라고 하지 않는다. 방송이 시작된 줄은 알지만 영상은 아직이다. */}
                    {({ waiting }) =>
                        waiting ? <span className="pill">곧 시작</span> : <span className="pill pill--live">LIVE</span>
                    }
                </HlsPlayer>

                <LiveInfo live={live} onChanged={reload} />
            </div>

            {/*
              지난 내역을 받은 뒤에 연결해야 순서가 꼬이지 않는다.
              key 로 방송이 바뀌면 채팅 상태를 새로 시작한다.
            */}
            {history && <ChatPanel key={id} live={live} history={history} me={me} />}
        </section>
    )
}

/**
 * 방송 제목·채널·구독·설명. 넓은 화면에서는 영상 아래에 모두 펼쳐 둔다.
 * 좁은 화면에서는 영상과 채팅이 한 화면에 함께 보이도록 제목과 채널만 남기고,
 * 구독과 설명은 "정보" 를 눌러야 펼쳐진다.
 */
function LiveInfo({ live, onChanged }) {
    const { me } = useAuth()
    const [open, setOpen] = useState(false)
    const moreId = `live-more-${live.id}`

    return (
        <div className={`live__info${open ? ' live__info--open' : ''}`}>
            <div className="live__head">
                <div className="live__titles">
                    <h1>{live.title}</h1>
                    <LiveMeta live={live} />
                </div>

                {/* 좁은 화면에서는 구독이 접혀 있으므로, 로그아웃이면 가장 큰 행동 하나만 머리 줄에 둔다. */}
                {!me && (
                    <Link
                        to={loginTo()}
                        className="cta cta--sm live__quick"
                        aria-label="로그인하고 이 채널 구독하기"
                    >
                        구독
                    </Link>
                )}

                <button
                    type="button"
                    className="live__toggle"
                    aria-expanded={open}
                    aria-controls={moreId}
                    onClick={() => setOpen((value) => !value)}
                >
                    {open ? '접기' : '정보'}
                </button>
            </div>

            <div id={moreId} className="live__more">
                <ChannelSubscribe channelId={live.channelId} onChanged={onChanged} identity />

                {live.description && <p className="description">{live.description}</p>}
            </div>
        </div>
    )
}

function LiveMeta({ live }) {
    return (
        <p className="meta">
            <Link to={{ view: 'channel', id: live.channelId }}>{live.nickname}</Link>
            {live.status === 'ENDED' && ' · 종료된 방송'}
            {live.audience && live.audience !== 'ALL' && LOCK_COPY[live.audience] && ` · ${LOCK_COPY[live.audience].title}`}
        </p>
    )
}

/** 영상을 볼 수 없는 방송. 구독 등급이 모자란 시청자에게 무엇을 하면 되는지 알려 준다. */
function LockedLive({ live, onChanged }) {
    const copy = LOCK_COPY[live.audience] ?? LOCK_COPY.SUBSCRIBERS

    return (
        <section className="locked-live">
            <h1>{live.title}</h1>

            <LiveMeta live={live} />

            <div className="locked-live__box">
                <p>
                    <strong>{copy.title}</strong>
                </p>
                <p className="meta">{copy.body}</p>

                <ChannelSubscribe channelId={live.channelId} onChanged={onChanged} />
            </div>
        </section>
    )
}

/** 끝난 방송. 다시보기가 남아 있으면 영상과 함께 방송 때의 채팅이 재생 위치에 맞춰 흐른다. */
function VodView({ live }) {
    const [currentTime, setCurrentTime] = useState(0)

    return (
        <section className="live">
            <div className="live__main">
                {live.vodUrl ? (
                    <HlsPlayer
                        src={live.vodUrl}
                        poster={live.thumbnailUrl}
                        label={`${live.title} 다시보기`}
                        onTimeUpdate={setCurrentTime}
                    />
                ) : (
                    <p className="empty">이 방송은 다시보기가 남아 있지 않습니다.</p>
                )}

                <LiveInfo live={live} />
            </div>

            {live.vodUrl && <ReplayChat key={live.id} liveId={live.id} currentTime={currentTime} />}
        </section>
    )
}
