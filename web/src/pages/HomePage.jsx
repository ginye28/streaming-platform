import { useEffect, useRef, useState } from 'react'
import {
    getCategories,
    getChannelLiveHistory,
    getLives,
    getMySubscriptions,
    getStreams,
} from '../api.js'
import { assetUrl } from '../assets.js'
import Link from '../components/Link.jsx'
import { HEART_COLORS, heartInk, toggleHeart, useHearts } from '../hearts.js'
import { useAsyncData } from '../useAsyncData.js'
import { useAuth } from '../useAuth.js'

const SORTS = [
    { value: 'LATEST', label: '최신순' },
    { value: 'POPULAR', label: '인기순' },
]

/**
 * 대회 중계 채널. 이 채널들 중 하나라도 방송 중이면 맨 위에 대회 배너(도는 슬롯)를 띄운다.
 * 대회가 없을 때는 배너 없이 "지금 방송 중" 줄과 도는 카드가 첫 화면이 된다.
 * 서버에 대회 개념이 생기기 전까지는 배포 설정(VITE_TOURNAMENT_CHANNEL_IDS)으로 정한다.
 */
const TOURNAMENT_CHANNEL_IDS = (import.meta.env.VITE_TOURNAMENT_CHANNEL_IDS ?? '')
    .split(',')
    .map((value) => Number(value.trim()))
    .filter((value) => Number.isInteger(value) && value > 0)

export default function HomePage() {
    const [categoryId, setCategoryId] = useState('')
    const [sortBy, setSortBy] = useState('LATEST')

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

    // 무료 서버는 한참 안 쓰면 잠든다. 첫 요청이 오래 걸리면 그 이유를 알려 준다.
    const waking = useSlow(livesLoading || loading)

    const liveList = lives?.content ?? []
    const tournament = liveList.filter((live) => TOURNAMENT_CHANNEL_IDS.includes(live.channelId))

    return (
        <section className="home">
            <h1 className="visually-hidden">홈</h1>

            {waking && (
                <p className="notice" role="status">
                    서버가 쉬고 있다가 깨는 중이에요. 처음 접속하면 30초 넘게 걸릴 수 있어요. 잠시만 기다려 주세요.
                </p>
            )}

            {tournament.length > 0 && (
                <HeroBanner live={tournament[0]} others={tournament.slice(1)} />
            )}

            <LiveStrip lives={liveList} />

            <LiveRing
                lives={liveList}
                loading={livesLoading}
                error={livesError}
                onRetry={reloadLives}
            />

            <div className="home__filters">
                <div className="home__filter-group" role="group" aria-label="카테고리">
                    <Chip active={categoryId === ''} onClick={() => setCategoryId('')}>
                        전체
                    </Chip>

                    {categories?.map((category) => (
                        <Chip
                            key={category.id}
                            active={categoryId === String(category.id)}
                            onClick={() => setCategoryId(String(category.id))}
                        >
                            {category.name}
                        </Chip>
                    ))}
                </div>

                <div className="home__filter-group home__filter-group--sort" role="group" aria-label="정렬">
                    {SORTS.map((sort) => (
                        <Chip
                            key={sort.value}
                            active={sortBy === sort.value}
                            onClick={() => setSortBy(sort.value)}
                        >
                            {sort.label}
                        </Chip>
                    ))}
                </div>
            </div>

            {error && (
                <p className="error">
                    {error} <button type="button" onClick={reload}>다시 시도</button>
                </p>
            )}
            {loading && <p className="empty">불러오는 중…</p>}

            {!loading && page?.content.length > 0 && (
                <Carousel title="새로 올라온 영상" items={page.content} />
            )}

            {!loading && !error && page?.content.length === 0 && (
                <EmptyVideos filtered={categoryId !== ''} onShowAll={() => setCategoryId('')} />
            )}
        </section>
    )
}

/** 가로 줄을 한 화면의 80% 만큼 넘긴다. 동작 줄이기를 켠 사람에게는 미끄러지지 않고 바로 넘긴다. */
function scrollTrack(track, direction) {
    if (!track) return

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    track.scrollBy({ left: direction * track.clientWidth * 0.8, behavior: reduceMotion ? 'auto' : 'smooth' })
}

/** active 가 delay 이상 이어지면 true. 느린 첫 응답에 안내를 띄울 때 쓴다. */
function useSlow(active, delay = 5000) {
    const [slow, setSlow] = useState(false)

    useEffect(() => {
        if (!active) return

        const timer = setTimeout(() => setSlow(true), delay)
        return () => {
            clearTimeout(timer)
            setSlow(false)
        }
    }, [active, delay])

    return slow
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
                <Link to={{ view: 'auth' }} className="empty__link">
                    로그인하고 첫 영상 올리기
                </Link>
            )}
        </p>
    )
}

/** 글자 기호(‹ › ▶) 대신 하트 아이콘과 같은 굵기(2)로 그린 아이콘. */
function Icon({ name, size = 18 }) {
    return (
        <svg
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill={name === 'play' ? 'currentColor' : 'none'}
            stroke={name === 'play' ? 'none' : 'currentColor'}
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
        >
            {name === 'prev' && <path d="M15 5l-7 7 7 7" />}
            {name === 'next' && <path d="M9 5l7 7-7 7" />}
            {name === 'play' && <path d="M7 4.5v15l12.5-7.5z" />}
        </svg>
    )
}

/* ── 2) 지금 방송 중 ───────────────────────────────────────────
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
                    <Icon name="prev" />
                </button>

                <ul className="strip__track" ref={trackRef}>
                    {onAir.map((live) => (
                        <li key={`live-${live.id}`}>
                            <Link to={{ view: 'live', id: live.id }} className="strip__person">
                                <span className="strip__face strip__face--live">
                                    {live.nickname?.slice(0, 1)}
                                </span>
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
                    <Icon name="next" />
                </button>
            </div>
        </section>
    )
}

/* ── 3) Live 중인 채널 ─────────────────────────────────────────
 * 스케치 모양: 왼쪽 아래 큰 카드 → 오른쪽으로 중간 카드 둘 → 그 뒤 위쪽에 작은 카드 셋이
 * 살짝 겹쳐 서고, 남는 카드는 큰 카드 뒤(왼쪽 위)에 숨어 있다가 앞으로 나온다.
 * 이 순서가 한 바퀴다. 오른쪽으로 끌면 가장 큰 카드가 다음 자리로 넘어가고,
 * 뒤에 있던 카드가 커지며 앞으로 온다. 하트를 누른 채널은 색이 붙고 누른 순서대로 앞에 선다.
 *
 * 자리는 폭 1100 을 기준으로 적어 두고, 실제 폭에 맞춰 같은 비율로 줄인다. */
const RING_BASE_W = 1100
const RING_BASE_H = 480
const RING_SLOTS = [
    { x: 20, y: 118, w: 470, o: 1, z: 60 },
    { x: 448, y: 210, w: 320, o: 1, z: 50 },
    { x: 736, y: 210, w: 320, o: 1, z: 45 },
    { x: 812, y: 50, w: 250, o: 1, z: 30 },
    { x: 590, y: 50, w: 250, o: 1, z: 25 },
    { x: 370, y: 50, w: 250, o: 1, z: 20 },
    { x: 190, y: 18, w: 230, o: 0.6, z: 12 },
    { x: 40, y: 50, w: 230, o: 0.4, z: 8 },
]
// 이보다 좁으면 겹쳐 도는 모양이 알아보기 어려워 가로 한 줄로 바꾼다.
const RING_MIN_W = 720
// 드래그 픽셀을 칸 수로 바꾸는 비율 (기준 폭에서).
const RING_PX_PER_SLOT = 220

function LiveRing({ lives, loading, error, onRetry }) {
    const hearts = useHearts()
    const boxRef = useRef(null)
    const dragRef = useRef(null)
    const [width, setWidth] = useState(RING_BASE_W)
    // 실제 폭을 재기 전의 카드는 그리지 않는다. 기준 폭 자리에서 실제 자리로
    // 날아오는 전환이 첫 화면마다 보이기 때문이다.
    const [measured, setMeasured] = useState(false)
    const [turn, setTurn] = useState(0)
    const [dragging, setDragging] = useState(false)
    const hasLives = lives.length > 0

    useEffect(() => {
        const el = boxRef.current
        if (!el) return

        const observer = new ResizeObserver(([entry]) => {
            setWidth(entry.contentRect.width)
            setMeasured(true)
        })
        observer.observe(el)
        return () => observer.disconnect()
        // 방송이 하나도 없을 때는 상자가 그려지지 않는다. 방송이 생기면 그때 붙잡는다.
    }, [hasLives])

    // 하트를 누른 순서대로 앞에, 나머지는 시청자가 많은 순서로 잇는다.
    const heartedLives = hearts
        .map((channelId) => lives.find((live) => live.channelId === channelId))
        .filter(Boolean)
    const ordered = [
        ...heartedLives,
        ...lives
            .filter((live) => !hearts.includes(live.channelId))
            .sort((a, b) => b.viewerCount - a.viewerCount),
    ]
    const n = ordered.length
    const scale = Math.min(1, width / RING_BASE_W)
    const flat = width < RING_MIN_W

    function handleHeart(channelId) {
        toggleHeart(channelId)
        // 하트 줄이 맨 앞부터 서도록 처음 자리로 돌린다.
        setTurn(0)
    }

    function handlePointerDown(event) {
        if (flat || event.target.closest('button')) return
        dragRef.current = { startX: event.clientX, startTurn: turn, moved: false, pointerId: event.pointerId }
    }

    function handlePointerMove(event) {
        const drag = dragRef.current
        if (!drag) return

        const delta = event.clientX - drag.startX

        // 살짝 흔들린 것까지 드래그로 치면 클릭이 죽는다. 넘어선 뒤에야 포인터를 붙잡는다.
        if (!drag.moved) {
            if (Math.abs(delta) <= 4) return
            drag.moved = true
            setDragging(true)
            boxRef.current?.setPointerCapture(drag.pointerId)
        }

        // 오른쪽으로 끌면(delta 양수) 가장 큰 카드가 다음 자리로 넘어간다.
        setTurn(drag.startTurn - delta / (RING_PX_PER_SLOT * scale))
    }

    function handlePointerUp() {
        const drag = dragRef.current
        if (!drag) return

        if (boxRef.current?.hasPointerCapture(drag.pointerId)) {
            boxRef.current.releasePointerCapture(drag.pointerId)
        }

        if (drag.moved) {
            setDragging(false)
            setTurn((current) => Math.round(current))
            // 끌고 난 직후의 클릭은 막고, 그다음 클릭부터는 받는다.
            setTimeout(() => {
                dragRef.current = null
            }, 0)
        } else {
            dragRef.current = null
        }
    }

    function handleClickCapture(event) {
        if (dragRef.current?.moved) {
            event.preventDefault()
            event.stopPropagation()
        }
    }

    if (n === 0 && (loading || error)) {
        return (
            <section className="ring-section" aria-labelledby="ring-title">
                <div className="section-head">
                    <h2 id="ring-title">Live 중인 채널</h2>
                </div>
                {error ? (
                    <p className="error">
                        방송 목록을 불러오지 못했어요. {error}{' '}
                        <button type="button" onClick={onRetry}>다시 시도</button>
                    </p>
                ) : (
                    <p className="empty" role="status">방송 목록을 불러오는 중…</p>
                )}
            </section>
        )
    }

    if (n === 0) {
        return (
            <section className="ring-section" aria-labelledby="ring-title">
                <div className="section-head">
                    <h2 id="ring-title">Live 중인 채널</h2>
                </div>
                <p className="empty">
                    지금 방송 중인 채널이 없어요.
                    <br />
                    <Link to={{ view: 'lives' }} className="empty__link">
                        예정된 방송 보기
                    </Link>
                </p>
            </section>
        )
    }

    return (
        <section className="ring-section" aria-labelledby="ring-title">
            <div className="section-head">
                <h2 id="ring-title">Live 중인 채널</h2>
                <span className="section-head__hint">
                    {flat
                        ? '옆으로 넘겨 보세요 · 하트를 누르면 색이 붙고 앞에 모여요'
                        : '끌면 가장 큰 카드가 다음 자리로 넘어가요 · 하트를 누르면 색이 붙고 누른 순서대로 앞에 모여요'}
                </span>

                {!flat && n > 1 && (
                    <span className="ring__arrows">
                        <button type="button" onClick={() => setTurn((t) => Math.round(t) + 1)} aria-label="이전 카드">
                            <Icon name="prev" />
                        </button>
                        <button type="button" onClick={() => setTurn((t) => Math.round(t) - 1)} aria-label="다음 카드">
                            <Icon name="next" />
                        </button>
                    </span>
                )}
            </div>

            <div
                ref={boxRef}
                className={`ring${flat ? ' ring--flat' : ''}${dragging ? ' ring--dragging' : ''}`}
                style={flat ? undefined : { height: RING_BASE_H * scale }}
                onPointerDown={handlePointerDown}
                onPointerMove={handlePointerMove}
                onPointerUp={handlePointerUp}
                onPointerCancel={handlePointerUp}
                onClickCapture={handleClickCapture}
            >
                {measured && ordered.map((live, pos) => {
                    const heartIndex = heartedLives.indexOf(live)
                    const color = heartIndex >= 0 ? HEART_COLORS[heartIndex % HEART_COLORS.length] : null

                    if (flat) {
                        return (
                            <RingCard
                                key={live.id}
                                live={live}
                                color={color}
                                order={heartIndex + 1}
                                size="md"
                                front
                                onHeart={handleHeart}
                            />
                        )
                    }

                    const f = mod(pos - turn, n)
                    const spot = ringPlace(f, n)
                    const w = spot.w * scale
                    const front = !dragging && Math.round(f) % n === 0

                    return (
                        <RingCard
                            key={live.id}
                            live={live}
                            color={color}
                            order={heartIndex + 1}
                            size={w >= 380 ? 'lg' : w >= 260 ? 'md' : 'sm'}
                            front={front}
                            onBringFront={() => setTurn(pos)}
                            onHeart={handleHeart}
                            style={{
                                width: w,
                                opacity: spot.o,
                                zIndex: 100 + spot.z,
                                transform: `translate(${spot.x * scale}px, ${spot.y * scale}px)`,
                            }}
                        />
                    )
                })}
            </div>
        </section>
    )
}

function RingCard({ live, color, order, size, front, onBringFront, onHeart, style }) {
    const hearted = color != null

    // 앞자리가 아닌 카드는 누르면 먼저 앞으로 불러온다. 한 번 더 누르면 방송으로 간다.
    const bring = front
        ? undefined
        : (event) => {
              event.preventDefault()
              onBringFront?.()
          }

    return (
        <div
            className={`ring__card ring__card--${size}${hearted ? ' ring__card--hearted' : ''}${front ? ' ring__card--front' : ''}`}
            style={{ ...style, ...(hearted ? { '--heart': color, '--on-heart': heartInk(color) } : {}) }}
        >
            <Link
                to={{ view: 'live', id: live.id }}
                className="ring__thumb"
                draggable="false"
                aria-label={front ? `${live.nickname} 방송 보기` : `${live.nickname} 카드를 앞으로`}
                {...(bring ? { onClick: bring } : {})}
            >
                {live.thumbnailUrl ? (
                    <img
                        src={assetUrl(live.thumbnailUrl)}
                        alt=""
                        draggable="false"
                        loading={front ? undefined : 'lazy'}
                        decoding="async"
                    />
                ) : (
                    <span className="ring__blank">{live.nickname?.slice(0, 1)}</span>
                )}
                <span className="ring__live">LIVE</span>
                {hearted && <span className="ring__order">{order}</span>}
            </Link>

            <div className="ring__info">
                <span className="ring__name">{live.nickname}</span>
                <span className="ring__title">{live.title}</span>
                <span className="ring__viewers">{formatCount(live.viewerCount)}</span>
            </div>

            {front && size !== 'md' && (
                <Link to={{ view: 'live', id: live.id }} className="ring__go" draggable="false">
                    <Icon name="play" size={11} /> 보러 가기
                </Link>
            )}

            <button
                type="button"
                className="ring__heart"
                aria-pressed={hearted}
                aria-label={`${live.nickname} 하트${hearted ? ' 빼기' : ''}`}
                onClick={() => onHeart(live.channelId)}
            >
                <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M12 20s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7.2a4.3 4.3 0 0 1 7.5 2.6C19.5 15.4 12 20 12 20z" />
                </svg>
            </button>
        </div>
    )
}

/**
 * 한 바퀴 위의 위치 f (0 = 맨 앞) 를 카드 자리로 바꾼다.
 * 자리보다 카드가 많으면 남는 카드는 마지막 자리 뒤에 숨어서 차례를 기다린다.
 */
function ringPlace(f, n) {
    const last = Math.min(n, RING_SLOTS.length) - 1

    const slotAt = (k) => {
        if (k >= n) return RING_SLOTS[0]
        if (k <= last) return RING_SLOTS[k]
        return { ...RING_SLOTS[last], o: 0, z: 0 }
    }

    const i = Math.floor(f)
    const t = f - i
    const a = slotAt(i)
    const b = slotAt(i + 1)
    const lerp = (key) => a[key] + (b[key] - a[key]) * t

    // 맨 뒤에서 맨 앞으로 넘어오는 카드는 반을 넘기면 가장 위로 올린다.
    const z = i + 1 >= n ? (t < 0.5 ? a.z : 70) : t < 0.5 ? a.z + 1 : b.z + 1

    return { x: lerp('x'), y: lerp('y'), w: lerp('w'), o: lerp('o'), z }
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

function mod(value, n) {
    return ((value % n) + n) % n
}

/** 서버는 시간대 없는 LocalDateTime 을 준다. 같은 시간대라고 보고 읽는다. */
function toTime(value, fallback = Date.now()) {
    const time = value ? new Date(value).getTime() : NaN
    return Number.isNaN(time) ? fallback : time
}

/** 3:41 처럼 시:분. */
function formatUptime(ms) {
    const minutes = Math.max(0, Math.floor(ms / 60000))
    return `${Math.floor(minutes / 60)}:${String(minutes % 60).padStart(2, '0')}`
}

function formatAgo(ms) {
    const minutes = Math.floor(ms / 60000)
    if (minutes < 60) return `${Math.max(1, minutes)}분 전`
    const hours = Math.floor(minutes / 60)
    if (hours < 24) return `${hours}시간 전`
    const days = Math.floor(hours / 24)
    if (days < 7) return days === 1 ? '어제' : `${days}일 전`
    if (days < 30) return `${Math.floor(days / 7)}주 전`
    return `${Math.floor(days / 30)}달 전`
}

// 맨 앞(가장 아래)까지 내려온 카드 한 장의 크기. 뒤로 갈수록 이보다 작아진다.
const CARD_W = 184
const CARD_H = 112
// 카드가 도는 타원. 중심은 배너 안쪽 오른편에 있고, 눕혀진 원이라
// 가로가 세로보다 넓다 — 위에서 비스듬히 내려다본 회전목마 모양이다.
// 타원을 키우면 오른쪽으로 삐져나가므로 중심도 같이 왼쪽으로 조금 당긴다.
const RING_CX = 0.67
const RING_CY = 0.45
const RING_RX = 0.26
const RING_RY = 0.36
// 카드 사이의 각도. 넓을수록 카드 사이가 벌어진다.
const SLOT_DEG = 40
// 맨 앞 카드가 서는 각도. 90 이 타원의 맨 아래다.
const FRONT_DEG = 90
// 이 각도 바깥은 타원의 뒤쪽(왼쪽 절반)이라 글자 뒤로 숨는다.
const FADE_IN_DEG = -108
const SOLID_FROM_DEG = -88
const SOLID_TO_DEG = 96
const FADE_OUT_DEG = 112
// 드래그 픽셀을 각도로 바꾸는 비율.
const DEG_PER_PX = 0.32

/**
 * 큰 배너는 고정이고, 그 위에서 작은 카드들만 눕혀진 타원을 따라 돈다.
 *
 * 타원의 중심은 배너 안쪽 오른편에 있고, 카드는 그 타원의 오른쪽 절반을
 * 지난다 — 오른쪽 위에서 작게 나타나 바깥으로 불룩하게 돌아 나갔다가,
 * 왼쪽 아래로 내려오며 점점 커진다. 가장 아래까지 내려온 카드가 맨 앞이다.
 * 왼쪽 절반은 제목이 있는 자리라 거기서는 흐려지며 숨는다.
 *
 * 자리는 각도를 카드 수로 나눈 나머지로 정하므로 끝까지 끌어도 처음
 * 카드로 돌아온다. 도는 방향은 부호 하나로 뒤집을 수 있다.
 */
function LiveWheel({ items }) {
    const ringRef = useRef(null)
    const dragRef = useRef(null)
    const [size, setSize] = useState({ width: 0, height: 420 })
    const [angle, setAngle] = useState(0)
    const [dragging, setDragging] = useState(false)

    useEffect(() => {
        const el = ringRef.current
        if (!el) return

        const observer = new ResizeObserver(([entry]) => {
            setSize({ width: entry.contentRect.width, height: entry.contentRect.height })
        })
        observer.observe(el)
        return () => observer.disconnect()
    }, [])

    const n = items.length

    function handlePointerDown(event) {
        dragRef.current = { startX: event.clientX, startAngle: angle, moved: false }
        setDragging(true)
        ringRef.current?.setPointerCapture(event.pointerId)
    }

    function handlePointerMove(event) {
        const drag = dragRef.current
        if (!drag) return

        const delta = event.clientX - drag.startX
        // 살짝 흔들린 것까지 드래그로 치면 클릭이 죽는다.
        if (Math.abs(delta) > 4) drag.moved = true
        // 오른쪽 → 왼쪽으로 끌면(delta 음수) 각도가 커져 바퀴가 돌아간다.
        setAngle(drag.startAngle - delta * DEG_PER_PX)
    }

    function handlePointerUp(event) {
        ringRef.current?.releasePointerCapture(event.pointerId)
        setDragging(false)
        // 놓으면 가장 가까운 카드가 맨 앞에 딱 서도록 스냅한다.
        setAngle((current) => Math.round(current / SLOT_DEG) * SLOT_DEG)
        dragRef.current = null
    }

    // 끌어서 돌린 직후의 클릭은 방송을 열지 않는다.
    function handleClickCapture(event) {
        if (dragRef.current?.moved) {
            event.preventDefault()
            event.stopPropagation()
        }
    }

    // 타원의 중심과 반지름. 배너 크기에 맞춰 함께 늘어난다.
    const cx = size.width * RING_CX
    const cy = size.height * RING_CY
    const rx = size.width * RING_RX
    const ry = size.height * RING_RY

    // 각도 0 은 타원의 오른쪽 끝, 90 은 맨 아래(=맨 앞)다.
    const slots = Math.ceil((FADE_OUT_DEG - FADE_IN_DEG) / SLOT_DEG)
    const base = Math.round(angle / SLOT_DEG)

    // 방송 수보다 자리가 많으면 같은 방송이 두 번 보인다. 그때는 맨 앞에 가까운
    // 쪽만 남긴다. 그래서 자리를 앞에서부터(k = 0) 바깥으로 훑는다.
    const order = [0]
    for (let d = 1; d <= slots; d++) order.push(d, -d)

    const taken = new Set()
    const cards = []

    for (const k of order) {
        // 슬롯 하나가 늘 정확히 맨 아래(FRONT_DEG)에 서도록 기준을 옮긴다.
        const theta = FRONT_DEG + (angle - base * SLOT_DEG) + k * SLOT_DEG
        if (theta < FADE_IN_DEG || theta > FADE_OUT_DEG) continue

        const rad = (theta * Math.PI) / 180
        const x = cx + rx * Math.cos(rad)
        const y = cy + ry * Math.sin(rad)

        // 아래로 내려올수록(=앞으로 나올수록) 커진다. 0 이 맨 위, 1 이 맨 아래.
        const depth = (Math.sin(rad) + 1) / 2
        const scale = 0.58 + 0.42 * depth

        // 타원의 왼쪽 절반은 제목이 있는 자리라 거기로 넘어가며 흐려진다.
        const opacity =
            theta < SOLID_FROM_DEG
                ? (theta - FADE_IN_DEG) / (SOLID_FROM_DEG - FADE_IN_DEG)
                : theta > SOLID_TO_DEG
                  ? (FADE_OUT_DEG - theta) / (FADE_OUT_DEG - SOLID_TO_DEG)
                  : 1

        const index = (((base + k) % n) + n) % n
        if (taken.has(index)) continue
        taken.add(index)

        const item = items[index]

        cards.push(
            <Link
                key={k}
                to={{ view: 'live', id: item.id }}
                className={`wheel__card${Math.abs(theta - FRONT_DEG) < SLOT_DEG / 2 ? ' wheel__card--front' : ''}`}
                draggable="false"
                style={{
                    width: CARD_W,
                    height: CARD_H,
                    opacity: Math.min(1, Math.max(0, opacity)),
                    zIndex: Math.round(100 + depth * 100),
                    transform: `translate(${x - CARD_W / 2}px, ${y - CARD_H / 2}px) scale(${scale})`,
                    transition: dragging ? 'none' : 'transform 340ms ease, opacity 340ms ease',
                }}
            >
                {item.thumbnailUrl ? (
                    <img src={assetUrl(item.thumbnailUrl)} alt="" draggable="false" />
                ) : (
                    <span className="wheel__blank">LIVE</span>
                )}
                <span className="wheel__title">{item.title}</span>
            </Link>
        )
    }

    return (
        <div
            className="wheel"
            ref={ringRef}
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={handlePointerUp}
            onPointerCancel={handlePointerUp}
            onClickCapture={handleClickCapture}
        >
            {size.width > 0 && cards}
        </div>
    )
}

/** 고정된 큰 배너. 여기는 돌지 않는다. */
function HeroBanner({ live, others }) {
    return (
        <div className="hero-wrap">
            {live.thumbnailUrl && (
                <img
                    className="hero-bg"
                    src={assetUrl(live.thumbnailUrl)}
                    alt=""
                    draggable="false"
                    fetchPriority="high"
                />
            )}

            <Link to={{ view: 'live', id: live.id }} className="hero-body" draggable="false">
                <div className="hero-badges">
                    <span className="pill pill--live">LIVE</span>
                    <span className="pill">시청자 {formatCount(live.viewerCount)}</span>
                </div>

                <h2 className="hero-title">{live.title}</h2>

                <span className="hero-channel">
                    <span className="hero-avatar" aria-hidden="true">
                        {live.nickname?.slice(0, 1)}
                    </span>
                    {live.nickname}
                </span>

                <span className="hero-cta">
                    <Icon name="play" size={12} /> 지금 보기
                </span>
            </Link>

            {others.length > 0 && <LiveWheel items={others} />}
        </div>
    )
}

/** 가로로 끌어서 넘기는 줄. 마우스로도 끌 수 있어야 해서 포인터를 직접 잡는다. */
function Carousel({ title, items }) {
    const trackRef = useRef(null)
    const dragRef = useRef(null)

    function handlePointerDown(event) {
        const track = trackRef.current
        if (!track) return

        dragRef.current = { startX: event.clientX, startLeft: track.scrollLeft, moved: false }
        track.setPointerCapture(event.pointerId)
    }

    function handlePointerMove(event) {
        const drag = dragRef.current
        const track = trackRef.current
        if (!drag || !track) return

        const moved = event.clientX - drag.startX
        // 살짝 흔들린 것까지 드래그로 치면 클릭이 죽는다.
        if (Math.abs(moved) > 4) drag.moved = true
        track.scrollLeft = drag.startLeft - moved
    }

    function handlePointerUp(event) {
        const track = trackRef.current
        if (track?.hasPointerCapture(event.pointerId)) {
            track.releasePointerCapture(event.pointerId)
        }
        dragRef.current = null
    }

    // 끌어서 넘긴 직후의 클릭은 열지 않는다.
    function handleClickCapture(event) {
        if (dragRef.current?.moved) {
            event.preventDefault()
            event.stopPropagation()
        }
    }

    return (
        <div className="carousel">
            <div className="carousel__head">
                <h3 className="carousel__title">{title}</h3>

                <span className="carousel__arrows">
                    <button type="button" onClick={() => scrollTrack(trackRef.current, -1)} aria-label="앞쪽 영상 보기">
                        <Icon name="prev" />
                    </button>
                    <button type="button" onClick={() => scrollTrack(trackRef.current, 1)} aria-label="뒤쪽 영상 보기">
                        <Icon name="next" />
                    </button>
                </span>
            </div>

            <ul
                className="carousel__track"
                ref={trackRef}
                onPointerDown={handlePointerDown}
                onPointerMove={handlePointerMove}
                onPointerUp={handlePointerUp}
                onPointerCancel={handlePointerUp}
                onClickCapture={handleClickCapture}
            >
                {items.map((stream) => (
                    <li key={stream.id} className="carousel__item">
                        <Link to={{ view: 'stream', id: stream.id }} className="tile" draggable="false">
                            <span className="tile__thumb">
                                {stream.thumbnailUrl ? (
                                    <img
                                        src={assetUrl(stream.thumbnailUrl)}
                                        alt=""
                                        draggable="false"
                                        loading="lazy"
                                        decoding="async"
                                    />
                                ) : (
                                    <span className="tile__blank">썸네일 없음</span>
                                )}
                            </span>

                            <span className="tile__title">{stream.title}</span>

                            <span className="tile__meta">
                                {stream.nickname} · 조회 {formatCount(stream.viewCount)}
                            </span>
                        </Link>
                    </li>
                ))}
            </ul>
        </div>
    )
}

function Chip({ active, onClick, children }) {
    return (
        <button
            type="button"
            className={`chip${active ? ' chip--on' : ''}`}
            aria-pressed={active}
            onClick={onClick}
        >
            {children}
        </button>
    )
}

/** 10000 → 1만. 자릿수가 커져도 배너 한 줄이 밀리지 않게 줄여 쓴다. */
function formatCount(value) {
    const count = value ?? 0

    if (count < 10000) return `${count.toLocaleString('ko-KR')}명`

    const man = count / 10000
    return `${man >= 100 ? Math.round(man) : man.toFixed(1).replace(/\.0$/, '')}만명`
}
