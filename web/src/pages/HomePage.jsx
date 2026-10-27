import { useEffect, useState } from 'react'
import { getCategories, getLives, getSchedules, getStreams } from '../api.js'
import Avatar from '../components/Avatar.jsx'
import CinemaHome from '../components/CinemaHome.jsx'
import { EmptyVideos, NoLive, NoLiveBar, StartGuide, SubscribedChannels } from '../components/HomeParts.jsx'
import { AUDIENCE_NOTE } from '../audience.js'
import Icon from '../components/Icon.jsx'
import Link from '../components/Link.jsx'
import { CardSkeleton, DurationBadge, HeartButton, LiveCard, Thumb, VideoCard } from '../components/MediaCards.jsx'
import Spinner from '../components/Spinner.jsx'
import { formatLiveFor, formatViewers } from '../format.js'
import { pruneHearts, useHearts } from '../hearts.js'
import { useLiveViewerCount } from '../liveViewers.js'
import { formatWhen, timeAgo } from '../time.js'
import { useAsyncData } from '../useAsyncData.js'
import { useTheme } from '../theme.js'
import { toTime, useNow } from '../useNow.js'
import { useSlow } from '../useSlow.js'

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
 * 홈. 이 화면이 답하는 질문은 하나다: "지금 무엇을 볼까?"
 *   1) 지금 방송 중인 것(가장 큰 한 칸 + 나머지 카드)  2) 곧 시작하는 방송  3) 올라온 영상(훑어보는 그리드)
 * 로그인했으면 구독 채널의 방송 여부가 칩 줄 바로 아래에 보인다.
 */
export default function HomePage() {
    const [categoryId, setCategoryId] = useState('')
    const [sortBy, setSortBy] = useState('LATEST')
    const hearts = useHearts()
    const theme = useTheme()

    const { data: categories } = useAsyncData(getCategories, [])
    const { data: lives, error: livesError, loading: livesLoading, reload: reloadLives } = useAsyncData(() => getLives(0), [])
    const { data: schedulePage, loading: scheduleLoading } = useAsyncData(() => getSchedules(0), [])
    const videos = useVideos(categoryId, sortBy)

    // 방송이 하나도 없을 때만 가장 많이 본 영상을 대표 카드로 올린다. 방송이 있으면 요청도 보내지 않는다.
    const noLive = !livesLoading && !livesError && (lives?.content ?? []).length === 0
    const { data: popularPage, loading: popularLoading } = useAsyncData(
        () => (noLive ? getStreams('', 'POPULAR', 0) : Promise.resolve(null)),
        [noLive]
    )
    const popular = popularPage?.content ?? []
    const topVideo = popular[0] ?? null

    // 무료 서버는 한참 안 쓰면 잠든다. 첫 요청이 오래 걸리면 그 이유를 알려 준다.
    const waking = useSlow(livesLoading || videos.loading)

    const liveList = lives?.content ?? []

    // 끝난 방송의 하트는 정리한다. 목록을 아직 못 받은 동안(빈 목록)에는 지우지 않는다.
    useEffect(() => {
        if (!livesLoading && !livesError) pruneHearts(liveList.map((live) => live.id))
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [lives, livesLoading, livesError])

    const ordered = orderLives(liveList, hearts)

    // 방송은 "전체"를 보고 있을 때만 보여 준다. 카테고리를 고르면 그 카테고리의 영상만 본다.
    const showLives = categoryId === ''

    // 나이트 모드를 켠 사람에게는 시네마 홈(와이드 슬라이더 + 벤토 격자)을 보여 준다. 데이터는 같다.
    if (theme === 'dark') {
        return (
            <CinemaHome
                categories={categories}
                categoryId={categoryId}
                onCategory={setCategoryId}
                lives={ordered}
                livesLoading={livesLoading}
                livesError={livesError}
                onRetryLives={reloadLives}
                popular={popular}
                popularLoading={noLive && popularLoading}
                schedules={schedulePage?.content ?? []}
                scheduleLoading={scheduleLoading}
                videos={videos}
                sortBy={sortBy}
                onSort={setSortBy}
                sorts={SORTS}
                waking={waking}
            />
        )
    }

    return (
        <div className="home">
            <h1 className="visually-hidden">홈</h1>

            <FilterBar categories={categories} categoryId={categoryId} onCategory={setCategoryId} />

            {waking && (
                <p className="notice home__notice" role="status">
                    <Spinner size={16} />
                    서버가 쉬고 있다가 깨는 중이에요. 처음 접속하면 30초 넘게 걸릴 수 있어요. 잠시만 기다려 주세요.
                </p>
            )}

            <SubscribedChannels />

            {showLives && (
                <LiveZone
                    lives={ordered}
                    loading={livesLoading}
                    error={livesError}
                    onRetry={reloadLives}
                    topVideo={topVideo}
                    topLoading={noLive && popularLoading}
                />
            )}

            {showLives && <UpcomingZone schedules={schedulePage?.content ?? []} loading={scheduleLoading} />}

            <VideoZone
                videos={videos}
                sortBy={sortBy}
                onSort={setSortBy}
                filtered={categoryId !== ''}
                onShowAll={() => setCategoryId('')}
            />
        </div>
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

/**
 * 영상 목록. 카테고리나 정렬이 바뀌면 처음부터 다시 받고, "더 보기"로 다음 쪽을 이어 붙인다.
 * 처음 한 쪽을 받는 동안(또는 조건이 바뀐 직후)은 loading 이다.
 */
function useVideos(categoryId, sortBy) {
    const key = `${categoryId}|${sortBy}`
    const [state, setState] = useState({ key: '', items: [], page: 0, last: true, error: null })
    const [more, setMore] = useState({ loading: false, error: null })
    const [attempt, setAttempt] = useState(0)

    useEffect(() => {
        let alive = true

        getStreams(categoryId, sortBy, 0).then(
            (page) => alive && setState({ key, items: page.content, page: 0, last: page.last, error: null }),
            (e) => alive && setState({ key, items: [], page: 0, last: true, error: e.message })
        )

        return () => {
            alive = false
        }
    }, [categoryId, sortBy, key, attempt])

    function loadMore() {
        setMore({ loading: true, error: null })

        getStreams(categoryId, sortBy, state.page + 1).then(
            (page) => {
                setState((prev) => {
                    if (prev.key !== key) return prev

                    const seen = new Set(prev.items.map((item) => item.id))

                    return {
                        ...prev,
                        items: [...prev.items, ...page.content.filter((item) => !seen.has(item.id))],
                        page: prev.page + 1,
                        last: page.last,
                    }
                })
                setMore({ loading: false, error: null })
            },
            (e) => setMore({ loading: false, error: e.message })
        )
    }

    function retry() {
        setState((prev) => ({ ...prev, key: '', error: null }))
        setAttempt((n) => n + 1)
    }

    return {
        items: state.items,
        loading: state.key !== key,
        error: state.error,
        last: state.last,
        more,
        loadMore,
        retry,
    }
}

/* ── 칩 줄: 머리 줄 바로 밑에 붙어 따라온다 ── */
function FilterBar({ categories, categoryId, onCategory }) {
    return (
        <div className="chips-bar">
            <div className="chips-bar__row" role="group" aria-label="카테고리">
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
        </div>
    )
}

function Chip({ active, onClick, children }) {
    return (
        <button type="button" className="chip" aria-pressed={active} onClick={onClick}>
            {children}
        </button>
    )
}

/* ── 지금 방송 중: 가장 큰 한 칸 + 나머지 카드 ── */
function LiveZone({ lives, loading, error, onRetry, topVideo, topLoading }) {
    const featured = lives[0] ?? null
    const rest = lives.slice(1)

    return (
        <section className="zone" aria-labelledby="live-title">
            <div className="shead">
                <h2 id="live-title">
                    지금 방송 중
                    {lives.length > 0 && <span className="shead__count num">{lives.length}</span>}
                </h2>
                <Link to={{ view: 'lives' }} className="shead__link">
                    라이브 전체 보기
                </Link>
            </div>

            {loading && <FeatureSkeleton />}

            {error && !loading && (
                <p className="error" role="alert">
                    방송 목록을 불러오지 못했어요. {error}{' '}
                    <button type="button" onClick={onRetry}>
                        다시 시도
                    </button>
                </p>
            )}

            {!loading && !error && !featured && topLoading && <FeatureSkeleton />}

            {/* 방송은 없지만 올라온 영상이 있으면: 한 줄 안내 + 가장 많이 본 영상. 둘 다 없으면 처음 오는 사람을 위한 시작 화면. */}
            {!loading && !error && !featured && !topLoading && topVideo && (
                <>
                    <NoLiveBar />
                    <FeaturedVideo stream={topVideo} />
                </>
            )}

            {!loading && !error && !featured && !topLoading && !topVideo && (
                <>
                    <NoLive />
                    <StartGuide />
                </>
            )}

            {featured && <FeaturedLive live={featured} />}

            {rest.length > 0 && (
                <ul className="vgrid vgrid--live" aria-label="다른 방송">
                    {rest.map((live, i) => (
                        <LiveCard key={live.id} live={live} index={i} />
                    ))}
                </ul>
            )}
        </section>
    )
}

function FeaturedLive({ live }) {
    const now = useNow()
    const viewers = useLiveViewerCount(live.id, live.viewerCount)
    const tournament = TOURNAMENT_CHANNEL_IDS.includes(live.channelId)
    const note = AUDIENCE_NOTE[live.audience]
    const uptime = formatLiveFor(now - toTime(live.startedAt))
    const peak = Math.max(live.peakViewerCount ?? 0, viewers)
    const extra = [tournament && '대회 중계', note].filter(Boolean).join(' · ')

    return (
        <article className="feature">
            <Link to={{ view: 'live', id: live.id }} className="feature__media" tabIndex={-1} aria-hidden="true" draggable="false">
                <Thumb src={live.thumbnailUrl} name={live.nickname} seed={live.channelId} play />
                <span className="badge badge--live">LIVE</span>
                <span className="badge badge--viewers num">
                    <Icon name="eye" size={14} />
                    {viewers.toLocaleString('ko-KR')}
                </span>
            </Link>

            <div className="feature__body">
                <Link to={{ view: 'channel', id: live.channelId }} className="feature__channel">
                    <Avatar name={live.nickname} src={live.profileImage} size={40} seed={live.channelId} live />
                    <span>{live.nickname}</span>
                </Link>

                <h3 className="feature__title">
                    <Link to={{ view: 'live', id: live.id }}>{live.title}</Link>
                </h3>

                {extra && <p className="feature__meta">{extra}</p>}

                {live.description && <p className="feature__desc">{live.description}</p>}

                <dl className="feature__stats num">
                    <div>
                        <dt>시청 중</dt>
                        <dd>{formatViewers(viewers)}</dd>
                    </div>
                    <div>
                        <dt>최고 시청</dt>
                        <dd>{formatViewers(peak)}</dd>
                    </div>
                    <div>
                        <dt>방송 시간</dt>
                        <dd>{uptime}</dd>
                    </div>
                </dl>

                <div className="feature__actions">
                    <Link to={{ view: 'live', id: live.id }} className="btn btn--primary btn--lg">
                        시청하기
                        <Icon name="right" size={20} className="btn__arrow" />
                    </Link>
                    <HeartButton liveId={live.id} nickname={live.nickname} className="heart-btn--solid" />
                </div>
            </div>
        </article>
    )
}

/** 방송이 없을 때 대표 자리에 올리는, 가장 많이 본 영상. 조회수 1위라는 사실만 말한다. */
function FeaturedVideo({ stream }) {
    const views = (stream.viewCount ?? 0).toLocaleString('ko-KR')
    const ago = timeAgo(stream.createdAt)

    return (
        <article className="feature feature--video">
            <Link to={{ view: 'stream', id: stream.id }} className="feature__media" tabIndex={-1} aria-hidden="true" draggable="false">
                <Thumb src={stream.thumbnailUrl} name={stream.nickname} seed={stream.userId} play />
                <span className="badge badge--pick">가장 많이 본 영상</span>
                <DurationBadge seconds={stream.durationSeconds} />
            </Link>

            <div className="feature__body">
                <Link to={{ view: 'channel', id: stream.userId }} className="feature__channel">
                    <Avatar name={stream.nickname} size={40} seed={stream.userId} />
                    <span>{stream.nickname}</span>
                </Link>

                <h3 className="feature__title">
                    <Link to={{ view: 'stream', id: stream.id }}>{stream.title}</Link>
                </h3>

                {stream.description && <p className="feature__desc">{stream.description}</p>}

                <dl className="feature__stats num">
                    <div>
                        <dt>조회수</dt>
                        <dd>{views}회</dd>
                    </div>
                    {ago && (
                        <div>
                            <dt>올린 때</dt>
                            <dd>{ago}</dd>
                        </div>
                    )}
                    {stream.categoryName && (
                        <div>
                            <dt>카테고리</dt>
                            <dd>{stream.categoryName}</dd>
                        </div>
                    )}
                </dl>

                <div className="feature__actions">
                    <Link to={{ view: 'stream', id: stream.id }} className="btn btn--primary btn--lg">
                        <Icon name="play" size={20} filled />
                        재생하기
                    </Link>
                </div>
            </div>
        </article>
    )
}

function FeatureSkeleton() {
    return (
        <div className="feature feature--skeleton" aria-hidden="true">
            <div className="feature__media skeleton" />
            <div className="feature__body">
                <span className="skeleton skeleton--line skeleton--short" />
                <span className="skeleton skeleton--line" />
                <span className="skeleton skeleton--line skeleton--short" />
            </div>
        </div>
    )
}

/* ── 곧 시작하는 방송: 없으면 자리를 차지하지 않는다 ── */
function UpcomingZone({ schedules, loading }) {
    const upcoming = schedules.slice(0, 4)

    if (loading || upcoming.length === 0) return null

    return (
        <section className="zone" aria-labelledby="upcoming-title">
            <div className="shead">
                <h2 id="upcoming-title">곧 시작하는 방송</h2>
                <Link to={{ view: 'lives' }} className="shead__link">
                    예정 방송 전체 보기
                </Link>
            </div>

            <ul className="sched-row">
                {upcoming.map((schedule) => (
                    <li key={schedule.id}>
                        <Link to={{ view: 'schedule', id: schedule.id }} className="sched">
                            <span className="sched__when num">
                                <Icon name="calendar" size={16} />
                                {formatWhen(schedule.scheduledAt)}
                            </span>
                            <h3 className="sched__title">{schedule.title}</h3>
                            <span className="sched__by">
                                <Avatar name={schedule.nickname} size={24} seed={schedule.channelId} />
                                {schedule.nickname}
                                {AUDIENCE_NOTE[schedule.audience] && ` · ${AUDIENCE_NOTE[schedule.audience]}`}
                            </span>
                        </Link>
                    </li>
                ))}
            </ul>
        </section>
    )
}

/* ── 올라온 영상 ── */
function VideoZone({ videos, sortBy, onSort, filtered, onShowAll }) {
    const { items, loading, error, last, more } = videos
    const empty = !loading && !error && items.length === 0

    return (
        <section className="zone" aria-labelledby="video-title">
            <div className="shead">
                <h2 id="video-title">영상</h2>

                <div className="seg" role="group" aria-label="정렬">
                    {SORTS.map((sort) => (
                        <button
                            key={sort.value}
                            type="button"
                            className="seg__item"
                            aria-pressed={sortBy === sort.value}
                            onClick={() => onSort(sort.value)}
                        >
                            {sort.label}
                        </button>
                    ))}
                </div>
            </div>

            {error && (
                <p className="error" role="alert">
                    영상 목록을 불러오지 못했어요. {error}{' '}
                    <button type="button" onClick={videos.retry}>
                        다시 시도
                    </button>
                </p>
            )}

            {loading && (
                <ul className="vgrid" aria-label="영상 목록을 불러오는 중">
                    {Array.from({ length: 8 }, (_, i) => (
                        <CardSkeleton key={i} index={i} />
                    ))}
                </ul>
            )}

            {!loading && items.length > 0 && (
                <ul className="vgrid">
                    {items.map((stream, i) => (
                        <VideoCard key={stream.id} stream={stream} index={i} />
                    ))}
                </ul>
            )}

            {empty && <EmptyVideos filtered={filtered} onShowAll={onShowAll} />}

            {!loading && items.length > 0 && !last && (
                <div className="vmore">
                    {more.error && <p className="error">{more.error}</p>}
                    <button type="button" className="btn btn--lg" onClick={videos.loadMore} disabled={more.loading}>
                        {more.loading ? <Spinner size={18} label="불러오는 중…" /> : '영상 더 보기'}
                    </button>
                </div>
            )}
        </section>
    )
}

