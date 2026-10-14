import { useEffect, useRef, useState } from 'react'
import {
    getCategories,
    getChannelLiveHistory,
    getLives,
    getMySubscriptions,
    getSchedules,
    getStreams,
} from '../api.js'
import { assetUrl } from '../assets.js'
import Avatar from '../components/Avatar.jsx'
import LeafLoader from '../components/LeafLoader.jsx'
import Link from '../components/Link.jsx'
import { loginTo } from '../router.js'
import { HEART_COLORS, pruneHearts, toggleHeart, useHearts } from '../hearts.js'
import { useLiveViewerCount } from '../liveViewers.js'
import { useAsyncData } from '../useAsyncData.js'
import { useAuth } from '../useAuth.js'
import { useSlow } from '../useSlow.js'
import { formatAgo, formatWhen, serverTimeMs, timeAgo } from '../time.js'

const SORTS = [
    { value: 'LATEST', label: '최신순' },
    { value: 'POPULAR', label: '인기순' },
]

/**
 * 대회 중계 채널. 이 채널들 중 하나라도 방송 중이면 맨 위 큰 화면에 먼저 올린다.
 * 서버에 대회 개념이 생기기 전까지는 배포 설정(VITE_TOURNAMENT_CHANNEL_IDS)으로 정한다.
 */
const TOURNAMENT_CHANNEL_IDS = (import.meta.env.VITE_TOURNAMENT_CHANNEL_IDS ?? '')
    .split(',')
    .map((value) => Number(value.trim()))
    .filter((value) => Number.isInteger(value) && value > 0)

/**
 * 필름스트립 카드의 크기. 가로·세로 비율이 번갈아 나와 높이가 들쭉날쭉하다.
 * r 은 나뭇잎의 큰 모서리 반지름이다(대각선으로 마주 보는 두 모서리가 이 값을 같이 쓴다).
 */
const FRAME_SIZES = [
    { w: 320, h: 196, r: 64 },
    { w: 196, h: 330, r: 84 },
    { w: 300, h: 184, r: 60 },
    { w: 200, h: 340, r: 88 },
    { w: 340, h: 200, r: 64 },
    { w: 190, h: 320, r: 80 },
]

export default function HomePage() {
    const [categoryId, setCategoryId] = useState('')
    const [sortBy, setSortBy] = useState('LATEST')
    const hearts = useHearts()

    const { data: categories } = useAsyncData(getCategories, [])
    const {
        data: lives,
        error: livesError,
        loading: livesLoading,
        reload: reloadLives,
    } = useAsyncData(() => getLives(0), [])
    const {
        data: page,
        error,
        loading,
        reload,
    } = useAsyncData(() => getStreams(categoryId, sortBy, 0), [categoryId, sortBy])
    const { data: schedulePage, loading: scheduleLoading } = useAsyncData(() => getSchedules(0), [])

    // 무료 서버는 한참 안 쓰면 잠든다. 첫 요청이 오래 걸리면 그 이유를 알려 준다.
    const waking = useSlow(livesLoading || loading)

    const liveList = lives?.content ?? []

    // 끝난 방송의 하트는 정리한다. 목록을 아직 못 받은 동안(빈 목록)에는 지우지 않는다.
    useEffect(() => {
        if (!livesLoading && !livesError) pruneHearts(liveList.map((live) => live.id))
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [lives, livesLoading, livesError])

    const ordered = orderLives(liveList, hearts)
    const heroLive = ordered[0] ?? null
    const restLives = ordered.slice(1)
    const videos = page?.content ?? []

    // 방송은 "전체"를 보고 있을 때만 필름스트립 앞에 섞는다. 카테고리를 고르면 그 카테고리의 영상만 본다.
    const items = [
        ...(categoryId === ''
            ? restLives.map((live) => ({ kind: 'live', key: `live-${live.id}`, live }))
            : []),
        ...videos.map((stream) => ({ kind: 'video', key: `video-${stream.id}`, stream })),
    ]

    return (
        <section className="c-home">
            {waking && (
                <p className="notice" role="status">
                    서버가 쉬고 있다가 깨는 중이에요. 처음 접속하면 30초 넘게 걸릴 수 있어요. 잠시만 기다려 주세요.
                </p>
            )}

            <Hero
                live={heroLive}
                liveCount={liveList.length}
                loading={livesLoading}
                error={livesError}
                onRetry={reloadLives}
            />

            <LiveStrip lives={liveList} />

            <Film
                categories={categories}
                categoryId={categoryId}
                onCategory={setCategoryId}
                sortBy={sortBy}
                onSort={setSortBy}
                items={items}
                loading={loading}
                error={error}
                onRetry={reload}
                videosEmpty={!loading && !error && videos.length === 0}
                hasLives={categoryId === '' && restLives.length > 0}
            />

            <Later schedules={schedulePage?.content ?? []} loading={scheduleLoading} />
        </section>
    )
}

/** 대회 중계 → 하트를 누른 순서 → 시청자 많은 순. */
function orderLives(lives, hearts) {
    const tournament = lives.filter((live) => TOURNAMENT_CHANNEL_IDS.includes(live.channelId))
    const rest = lives.filter((live) => !TOURNAMENT_CHANNEL_IDS.includes(live.channelId))
    const hearted = hearts.map((id) => rest.find((live) => live.id === id)).filter(Boolean)
    const others = rest.filter((live) => !hearts.includes(live.id)).sort((a, b) => b.viewerCount - a.viewerCount)

    return [...tournament, ...hearted, ...others]
}

/* ── 1) 큰 화면: 화면 전체가 하나의 재생 버튼이다. 위에 얹힌 버튼은 없다 ── */
function Hero({ live, liveCount, loading, error, onRetry }) {
    const { me } = useAuth()
    const now = useNow()
    const hearts = useHearts()
    const today = new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())

    if (!live) {
        // 방송이 없을 때도 같은 자리, 같은 크기. 이유와 다음 행동을 알려 준다.
        let headline = '오늘의 방송'
        if (!loading && !error) headline = liveCount > 0 ? `지금 ${liveCount}곳이 방송 중이에요` : '지금은 조용해요'

        return (
            <article className="c-hero c-hero--empty" aria-labelledby="hero-title">
                <div className="c-hero__frame">
                    <div className="c-hero__scene" />
                    <div className="c-hero__shade" />
                </div>

                <div className="c-hero__info">
                    <p className="c-hero__kicker">{today}</p>
                    <h1 className="c-hero__title" id="hero-title">{headline}</h1>

                    {loading && <LeafLoader size={22} label="방송 목록을 불러오는 중…" />}

                    {error && (
                        <p className="c-hero__by">
                            <span>방송 목록을 불러오지 못했어요. {error}</span>
                            <button type="button" className="c-hero__action" onClick={onRetry}>
                                다시 시도
                            </button>
                        </p>
                    )}

                    {!loading && !error && (
                        <p className="c-hero__by">
                            <Link to={{ view: 'lives' }} className="c-hero__action">
                                예정된 방송 보기
                            </Link>
                            <Link to={me ? { view: 'broadcast' } : loginTo()} className="c-hero__action">
                                {me ? '방송하기' : '로그인하고 방송하기'}
                            </Link>
                        </p>
                    )}
                </div>
            </article>
        )
    }

    const tournament = TOURNAMENT_CHANNEL_IDS.includes(live.channelId)
    const hearted = hearts.includes(live.id)
    const heartColor = hearted ? HEART_COLORS[hearts.indexOf(live.id) % HEART_COLORS.length] : null

    return (
        <article className="c-hero" aria-labelledby="hero-title">
            <div className="c-hero__frame">
                <div className="c-hero__scene" />
                {live.thumbnailUrl && (
                    <img className="c-hero__img" src={assetUrl(live.thumbnailUrl)} alt="" fetchPriority="high" decoding="async" />
                )}
                <div className="c-hero__shade" />
            </div>

            {/* 화면 어디를 눌러도 바로 시청 화면으로 간다 */}
            <Link to={{ view: 'live', id: live.id }} className="c-hero__hit" aria-label={`${live.title} 방송 바로 보기`} draggable="false" />

            <div className="c-hero__top">
                <span className="c-badge-live">
                    <i aria-hidden="true" />
                    LIVE
                </span>
                <HeroViewers live={live} />
                {tournament && <span className="c-chip-soft c-chip-soft--tag">대회 중계</span>}
            </div>

            <div className="c-hero__info">
                <p className="c-hero__kicker">{tournament ? '대회 중계 · 지금 방송 중' : '지금 방송 중'}</p>
                <h1 className="c-hero__title" id="hero-title">{live.title}</h1>

                <p className="c-hero__by">
                    <span className="c-hero__who">
                        <Avatar name={live.nickname} size={30} src={live.profileImage} />
                        {live.nickname} · {formatUptime(now - toTime(live.startedAt))}째
                    </span>
                    <span className="c-hero__cue">▶ 화면을 누르면 바로 시청</span>
                    <button
                        type="button"
                        className="c-heart c-hero__heart"
                        aria-pressed={hearted}
                        aria-label={`${live.nickname} 방송 하트${hearted ? ' 빼기' : ''}`}
                        style={heartColor ? { '--heart': heartColor } : undefined}
                        onClick={() => toggleHeart(live.id)}
                    >
                        <HeartIcon />
                    </button>
                </p>
            </div>
        </article>
    )
}

/* ── 2) 지금 방송 중 줄 ───────────────────────────────────────────
 * 방송 중인 채널이 오래 방송한 순서로 서고, 오른쪽으로 넘기면 "쉬는 중" 뒤로
 * 구독한 채널 중 방송하지 않는 채널이 흐리게 이어진다. 최근에 끝난 채널이 먼저,
 * 오래 쉰 채널일수록 오른쪽 끝이다. */
function LiveStrip({ lives }) {
    const { me } = useAuth()
    const trackRef = useRef(null)
    const now = useNow()

    const onAir = [...lives].sort((a, b) => toTime(a.startedAt) - toTime(b.startedAt))

    // 쉬는 채널은 로그인해서 구독한 채널이 있을 때만 알 수 있다.
    // 마지막 방송이 언제 끝났는지는 채널마다 지난 방송 첫 장으로 잰다.
    const { data: resting } = useAsyncData(async () => {
        if (!me) return []

        const subscriptions = await getMySubscriptions(0)
        const offline = subscriptions.content.filter((channel) => !channel.live)

        const withLast = await Promise.all(
            offline.map(async (channel) => {
                const history = await getChannelLiveHistory(channel.id, 0).catch(() => null)
                return { ...channel, lastEndedAt: history?.content?.[0]?.endedAt ?? null }
            })
        )

        // 한 번도 방송하지 않은 채널은 맨 끝.
        return withLast.sort((a, b) => toTime(b.lastEndedAt, 0) - toTime(a.lastEndedAt, 0))
    }, [me?.id])

    function scroll(direction) {
        scrollTrack(trackRef.current, direction)
    }

    if (onAir.length === 0 && !resting?.length) return null

    return (
        <section className="strip" aria-labelledby="strip-title">
            <div className="section-head">
                <h2 id="strip-title">지금 방송 중</h2>
                <span className="section-head__hint">
                    오래 방송한 순{resting?.length > 0 && ' · 오른쪽 끝은 오래 쉰 채널'}
                </span>
            </div>

            <div className="strip__row">
                <button type="button" className="strip__arrow" onClick={() => scroll(-1)} aria-label="앞쪽 보기">
                    <ChevronIcon dir="prev" />
                </button>

                <ul className="strip__track" ref={trackRef}>
                    {onAir.map((live) => (
                        <li key={`live-${live.id}`}>
                            <Link to={{ view: 'live', id: live.id }} className="strip__person">
                                <span className="strip__face strip__face--live">{live.nickname?.slice(0, 1)}</span>
                                <span className="strip__name">{live.nickname}</span>
                                <span className="strip__meta strip__meta--live">
                                    {formatUptime(now - toTime(live.startedAt))}째
                                </span>
                            </Link>
                        </li>
                    ))}

                    {resting?.length > 0 && (
                        <li className="strip__divider" aria-hidden="true">
                            <span />
                            쉬는 중
                        </li>
                    )}

                    {resting?.map((channel) => (
                        <li key={`rest-${channel.id}`}>
                            <Link
                                to={{ view: 'channel', id: channel.id }}
                                className="strip__person strip__person--resting"
                            >
                                <span className="strip__face">
                                    {channel.profileImage ? (
                                        <img src={assetUrl(channel.profileImage)} alt="" loading="lazy" decoding="async" />
                                    ) : (
                                        channel.nickname.slice(0, 1)
                                    )}
                                </span>
                                <span className="strip__name">{channel.nickname}</span>
                                <span className="strip__meta">
                                    {channel.lastEndedAt ? formatAgo(now - toTime(channel.lastEndedAt)) : '방송 전'}
                                </span>
                            </Link>
                        </li>
                    ))}
                </ul>

                <button type="button" className="strip__arrow" onClick={() => scroll(1)} aria-label="뒤쪽 보기">
                    <ChevronIcon dir="next" />
                </button>
            </div>
        </section>
    )
}

/* ── 3) 필름스트립: 나뭇잎 모양 카드를 가로로 넘긴다 ── */
function Film({
    categories,
    categoryId,
    onCategory,
    sortBy,
    onSort,
    items,
    loading,
    error,
    onRetry,
    videosEmpty,
    hasLives,
}) {
    const trackRef = useRef(null)
    const dragRef = useRef(null)
    const [atEnd, setAtEnd] = useState(false)

    function updateEnd() {
        const track = trackRef.current
        if (!track) return
        setAtEnd(track.scrollLeft + track.clientWidth >= track.scrollWidth - 4)
    }

    // 항목이 바뀌면 처음으로 돌아가고 끝 표시를 다시 잰다.
    useEffect(() => {
        trackRef.current?.scrollTo({ left: 0 })
        updateEnd()
    }, [items.length, categoryId, sortBy])

    function handlePointerDown(event) {
        const track = trackRef.current
        if (!track || event.pointerType === 'touch') return
        dragRef.current = { startX: event.clientX, startLeft: track.scrollLeft, moved: false }
    }

    function handlePointerMove(event) {
        const drag = dragRef.current
        const track = trackRef.current
        if (!drag || !track) return

        const moved = event.clientX - drag.startX
        // 살짝 흔들린 것까지 드래그로 치면 클릭이 죽는다.
        if (Math.abs(moved) > 5) {
            if (!drag.moved) track.setPointerCapture(event.pointerId)
            drag.moved = true
        }
        if (drag.moved) track.scrollLeft = drag.startLeft - moved
    }

    function handlePointerUp(event) {
        const track = trackRef.current
        if (track?.hasPointerCapture(event.pointerId)) track.releasePointerCapture(event.pointerId)
        // 끌어서 넘긴 직후의 클릭은 열지 않는다. 다음 틱에 비운다.
        setTimeout(() => {
            dragRef.current = null
        }, 0)
    }

    function handleClickCapture(event) {
        if (dragRef.current?.moved) {
            event.preventDefault()
            event.stopPropagation()
        }
    }

    function handleKeyDown(event) {
        if (event.key === 'ArrowRight') scrollTrack(trackRef.current, 1)
        if (event.key === 'ArrowLeft') scrollTrack(trackRef.current, -1)
    }

    return (
        <section className="c-film" aria-labelledby="film-title">
            <div className="c-film__head">
                <h2 className="c-film__title" id="film-title">필름스트립</h2>

                <div className="c-tabs" role="group" aria-label="카테고리">
                    <Chip active={categoryId === ''} onClick={() => onCategory('')}>
                        전체
                    </Chip>
                    {categories?.map((category) => (
                        <Chip
                            key={category.id}
                            active={categoryId === String(category.id)}
                            onClick={() => onCategory(String(category.id))}
                        >
                            {category.name}
                        </Chip>
                    ))}
                </div>

                <div className="c-tabs c-tabs--sort" role="group" aria-label="정렬">
                    {SORTS.map((sort) => (
                        <Chip key={sort.value} active={sortBy === sort.value} onClick={() => onSort(sort.value)}>
                            {sort.label}
                        </Chip>
                    ))}
                </div>

                {items.length > 0 && (
                    <div className="c-film__nav">
                        <button type="button" className="c-round" aria-label="앞쪽으로" data-tip="앞쪽으로" onClick={() => scrollTrack(trackRef.current, -1)}>
                            <ChevronIcon dir="prev" />
                        </button>
                        <button type="button" className="c-round" aria-label="뒤쪽으로" data-tip="뒤쪽으로" onClick={() => scrollTrack(trackRef.current, 1)}>
                            <ChevronIcon dir="next" />
                        </button>
                    </div>
                )}
            </div>

            {error && (
                <p className="error">
                    {error} <button type="button" onClick={onRetry}>다시 시도</button>
                </p>
            )}

            {loading && items.length === 0 && (
                <p className="empty">
                    <LeafLoader size={26} label="불러오는 중…" />
                </p>
            )}

            {items.length > 0 && (
                <div className="c-reel">
                    <ul
                        className={`c-track${atEnd ? ' c-track--end' : ''}`}
                        ref={trackRef}
                        tabIndex={0}
                        aria-label="영상 목록, 좌우로 끌어서 넘기기"
                        onScroll={updateEnd}
                        onKeyDown={handleKeyDown}
                        onPointerDown={handlePointerDown}
                        onPointerMove={handlePointerMove}
                        onPointerUp={handlePointerUp}
                        onPointerCancel={handlePointerUp}
                        onClickCapture={handleClickCapture}
                    >
                        {items.map((item, index) => (
                            <Frame key={item.key} item={item} index={index} />
                        ))}
                    </ul>
                </div>
            )}

            {videosEmpty && !hasLives && <EmptyVideos filtered={categoryId !== ''} onShowAll={() => onCategory('')} />}
            {videosEmpty && hasLives && (
                <p className="c-film__note">올라온 영상은 아직 없어요. 지금 방송 중인 채널을 먼저 보여 드려요.</p>
            )}
        </section>
    )
}

function Frame({ item, index }) {
    const size = FRAME_SIZES[index % FRAME_SIZES.length]
    // 짝수는 왼쪽위·오른쪽아래가 큰 잎, 홀수는 좌우를 뒤집은 잎.
    const flip = index % 2 === 1

    if (item.kind === 'live') {
        return <LiveFrame live={item.live} size={size} flip={flip} />
    }

    const { stream } = item

    return (
        <li
            className={`c-frame${flip ? ' c-frame--b' : ''}`}
            style={{ '--w': `${size.w}px`, '--h': `${size.h}px`, '--Rb': `${size.r}px` }}
        >
            <Link to={{ view: 'stream', id: stream.id }} className="c-frame__link" draggable="false">
                <FrameArt src={stream.thumbnailUrl} name={stream.nickname} />
                <span className="c-frame__cap">
                    <span className="c-frame__cat">
                        {stream.categoryName ?? '영상'} · 조회 {(stream.viewCount ?? 0).toLocaleString('ko-KR')}회
                        {stream.createdAt && ` · ${timeAgo(stream.createdAt)}`}
                    </span>
                    <span className="c-frame__name">
                        <Avatar name={stream.nickname} size={22} className="c-avatar--inline" />
                        {stream.title}
                    </span>
                </span>
            </Link>

            <Holo />
        </li>
    )
}

/**
 * 방송 중인 카드. 시청자 수는 WebSocket 으로 밀려 오는 값을 쓰고, 마우스를 올리거나 포커스하면
 * "방송 시간 · 지금 시청 · 최고 시청" 진행 상태가 아래에서 펼쳐진다.
 */
function LiveFrame({ live, size, flip }) {
    const hearts = useHearts()
    const now = useNow()
    const viewers = useLiveViewerCount(live.id, live.viewerCount)
    const hearted = hearts.includes(live.id)
    const color = hearted ? HEART_COLORS[hearts.indexOf(live.id) % HEART_COLORS.length] : null
    const peak = Math.max(live.peakViewerCount ?? 0, viewers)
    const uptime = formatUptimeKorean(now - toTime(live.startedAt))

    return (
        <li
            className={`c-frame${flip ? ' c-frame--b' : ''}`}
            style={{ '--w': `${size.w}px`, '--h': `${size.h}px`, '--Rb': `${size.r}px` }}
        >
            <Link to={{ view: 'live', id: live.id }} className="c-frame__link" draggable="false">
                <FrameArt src={live.thumbnailUrl} name={live.nickname} />
                <span className="c-frame__cap">
                    <span className="c-frame__cat">LIVE · {formatCount(viewers)}</span>
                    <span className="c-frame__name">
                        <Avatar name={live.nickname} size={22} src={live.profileImage} className="c-avatar--inline" />
                        {live.title}
                    </span>
                    <span className="c-frame__progress">
                        <span className="c-frame__progress-inner">
                            {uptime} · 지금 {viewers.toLocaleString('ko-KR')}명 · 최고 {peak.toLocaleString('ko-KR')}명
                        </span>
                    </span>
                </span>
            </Link>

            <Holo />
            <span className="c-voice" aria-hidden="true">
                <b /><b /><b /><b /><b />
                <em>LIVE</em>
            </span>
            <button
                type="button"
                className="c-heart c-frame__heart"
                aria-pressed={hearted}
                aria-label={`${live.nickname} 방송 하트${hearted ? ' 빼기' : ''}`}
                style={color ? { '--heart': color } : undefined}
                onClick={() => toggleHeart(live.id)}
            >
                <HeartIcon />
            </button>
        </li>
    )
}

function FrameArt({ src, name }) {
    return (
        <>
            <span className="c-frame__art">
                {src ? (
                    <img src={assetUrl(src)} alt="" draggable="false" loading="lazy" decoding="async" />
                ) : (
                    <span className="c-frame__blank">{name?.slice(0, 1)}</span>
                )}
            </span>
            <span className="c-frame__shade" />
        </>
    )
}

/** 각진 모서리에 앉는 작은 홀로그램 재생 표시. 장식이고, 누르는 건 카드 전체다. */
function Holo() {
    return (
        <span className="c-holo" aria-hidden="true">
            <svg viewBox="0 0 24 24">
                <path d="M6 4v16l14-8z" />
            </svg>
        </span>
    )
}

/** 올라온 영상이 없을 때. 비어 있는 이유와 다음 행동을 함께 보여 준다. */
function EmptyVideos({ filtered, onShowAll }) {
    const { me } = useAuth()

    if (filtered) {
        return (
            <p className="empty">
                이 카테고리에는 아직 영상이 없어요.
                <br />
                <button type="button" className="empty__link" onClick={onShowAll}>
                    전체 영상 보기
                </button>
            </p>
        )
    }

    return (
        <p className="empty">
            아직 올라온 영상이 없어요.
            <br />
            {me ? (
                <Link to={{ view: 'upload' }} className="empty__link">
                    첫 영상 올리기
                </Link>
            ) : (
                <Link to={loginTo()} className="empty__link">
                    로그인하고 첫 영상 올리기
                </Link>
            )}
        </p>
    )
}

/* ── 4) 예정된 방송: 방송 구역과 떨어진 맨 아래. 카드가 겹쳐 서 있다 ── */
const AUDIENCE_NOTE = {
    SUBSCRIBERS: '구독자 전용',
    PAID: '유료 구독자 전용',
}

function Later({ schedules, loading }) {
    const upcoming = schedules.slice(0, 4)

    return (
        <aside className="c-later" aria-labelledby="later-title">
            <div className="c-later__head">
                <p className="c-later__eyebrow">NEXT</p>
                <h2 className="c-later__title" id="later-title">곧 시작하는 방송</h2>
                <p className="c-later__sub">
                    <Link to={{ view: 'lives' }} className="c-later__link">
                        예정 방송 전체 보기
                    </Link>
                </p>
            </div>

            {loading && upcoming.length === 0 && (
                <p className="empty">
                    <LeafLoader size={22} label="불러오는 중…" />
                </p>
            )}

            {!loading && upcoming.length === 0 && (
                <p className="c-later__empty">예정된 방송이 아직 없어요. 스트리머가 방송을 예약하면 여기에 모여요.</p>
            )}

            {upcoming.length > 0 && (
                <div className="c-later__row">
                    {upcoming.map((schedule) => (
                        <Tier key={schedule.id} schedule={schedule} />
                    ))}
                </div>
            )}
        </aside>
    )
}

function Tier({ schedule }) {
    const ref = useRef(null)

    // 마우스 위치에 따라 카드가 살짝 기운다. 움직임 줄이기에서는 CSS 가 막는다.
    function tilt(event) {
        const el = ref.current
        if (!el) return
        const r = el.getBoundingClientRect()
        const px = (event.clientX - r.left) / r.width - 0.5
        const py = (event.clientY - r.top) / r.height - 0.5
        el.style.setProperty('--ry', `${(px * 9).toFixed(2)}deg`)
        el.style.setProperty('--rx', `${(-py * 7).toFixed(2)}deg`)
    }

    function untilt() {
        ref.current?.style.setProperty('--ry', '0deg')
        ref.current?.style.setProperty('--rx', '0deg')
    }

    return (
        <Link
            to={{ view: 'schedule', id: schedule.id }}
            className="c-tier"
            ref={ref}
            onPointerMove={tilt}
            onPointerLeave={untilt}
        >
            <p className="c-tier__label">{formatWhen(schedule.scheduledAt)} · 예정</p>
            <h3 className="c-tier__title">{schedule.title}</h3>
            <p className="c-tier__meta">
                {schedule.nickname}
                {AUDIENCE_NOTE[schedule.audience] && ` · ${AUDIENCE_NOTE[schedule.audience]}`}
            </p>
            <Avatar name={schedule.nickname} size={34} className="c-tier__avatar" />
            <span className="c-sparks" aria-hidden="true">
                <i /><i /><i /><i /><i /><i />
            </span>
        </Link>
    )
}

/* ── 작은 부품들 ── */

function Chip({ active, onClick, children }) {
    return (
        <button type="button" className={`chip${active ? ' chip--on' : ''}`} aria-pressed={active} onClick={onClick}>
            {children}
        </button>
    )
}

function HeartIcon() {
    return (
        <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 20s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7.2a4.3 4.3 0 0 1 7.5 2.6C19.5 15.4 12 20 12 20z" />
        </svg>
    )
}

function ChevronIcon({ dir }) {
    return (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d={dir === 'prev' ? 'M15 5l-7 7 7 7' : 'M9 5l7 7-7 7'} />
        </svg>
    )
}

/** 가로 줄을 한 화면의 80% 만큼 넘긴다. 동작 줄이기를 켠 사람에게는 미끄러지지 않고 바로 넘긴다. */
function scrollTrack(track, direction) {
    if (!track) return

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    track.scrollBy({ left: direction * track.clientWidth * 0.8, behavior: reduceMotion ? 'auto' : 'smooth' })
}

/** 지금 시각. 방송 시간이 흘러가 보이도록 1분마다 새로 잰다. */
function useNow() {
    const [now, setNow] = useState(() => Date.now())

    useEffect(() => {
        const timer = setInterval(() => setNow(Date.now()), 60_000)
        return () => clearInterval(timer)
    }, [])

    return now
}

/** 서버 시각(시간대 표시 없는 UTC)을 밀리초로. 읽을 수 없으면 fallback. */
function toTime(value, fallback = Date.now()) {
    return serverTimeMs(value, fallback)
}

/** 3:41 처럼 시:분. */
function formatUptime(ms) {
    const minutes = Math.max(0, Math.floor(ms / 60000))
    return `${Math.floor(minutes / 60)}:${String(minutes % 60).padStart(2, '0')}`
}

/** 큰 화면의 시청자 수. 서버가 밀어 주는 값으로 바로 바뀐다. */
function HeroViewers({ live }) {
    const viewers = useLiveViewerCount(live.id, live.viewerCount)

    return <span className="c-chip-soft">시청 {formatCount(viewers)}</span>
}

/** 방송 시간을 말로. 막 시작했으면 "방금 시작", 아니면 "12분째 방송" · "1시간 5분째 방송". */
function formatUptimeKorean(ms) {
    const minutes = Math.max(0, Math.floor(ms / 60000))

    if (minutes < 1) return '방금 시작'

    const hours = Math.floor(minutes / 60)
    const rest = minutes % 60

    if (hours === 0) return `${rest}분째 방송`

    return rest === 0 ? `${hours}시간째 방송` : `${hours}시간 ${rest}분째 방송`
}

/** 10000 → 1만. 자릿수가 커져도 한 줄이 밀리지 않게 줄여 쓴다. */
function formatCount(value) {
    const count = value ?? 0

    if (count < 10000) return `${count.toLocaleString('ko-KR')}명`

    const man = count / 10000
    return `${man >= 100 ? Math.round(man) : man.toFixed(1).replace(/\.0$/, '')}만명`
}
