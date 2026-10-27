import { useEffect, useState } from 'react'
import { assetUrl } from '../assets.js'
import { AUDIENCE_NOTE } from '../audience.js'
import { formatLiveFor, formatViews } from '../format.js'
import { useLiveViewerCount } from '../liveViewers.js'
import { tintOf } from '../tint.js'
import { formatWhen, timeAgo } from '../time.js'
import { toTime, useNow } from '../useNow.js'
import Avatar from './Avatar.jsx'
import { EmptyVideos, NoLive, NoLiveBar, StartGuide, SubscribedChannels } from './HomeParts.jsx'
import Icon from './Icon.jsx'
import Link from './Link.jsx'
import { tileSize } from '../bento.js'
import { BentoItem } from './Bento.jsx'
import { DurationBadge, Thumb } from './MediaCards.jsx'
import Spinner from './Spinner.jsx'

/** 슬라이더가 넘어가는 간격. 마우스를 올리거나 포커스하면 멈춘다. */
const SLIDE_MS = 7000
const HERO_LIVES = 5
const HERO_VIDEOS = 4

/**
 * 나이트 모드의 홈: 와이드 시네마 슬라이더 + 크기가 제각각인 벤토 격자.
 * 보여 주는 것은 모두 서버에 있는 방송·영상·예정·카테고리뿐이다. 시청자 수나 채팅 같은 걸 지어내지 않고,
 * 없으면 빈 상태 안내를 보여 준다. 불러오는 동안은 같은 격자 모양의 뼈대를 먼저 깐다.
 */
export default function CinemaHome({
    categories,
    categoryId,
    onCategory,
    lives,
    livesLoading,
    livesError,
    onRetryLives,
    popular,
    popularLoading,
    schedules,
    scheduleLoading,
    videos,
    sortBy,
    onSort,
    sorts,
    waking,
}) {
    // 방송은 "전체"를 보고 있을 때만 앞세운다. 카테고리를 고르면 그 카테고리의 영상만 본다.
    const showHero = categoryId === ''
    const slides = buildSlides(lives, popular)
    const heroIds = new Set(slides.map((slide) => slide.key))
    const heroLoading = livesLoading || popularLoading
    const noContent = !heroLoading && !livesError && slides.length === 0 && !videos.loading && videos.items.length === 0

    // 슬라이더에 올라간 것은 격자에서 뺀다(방송이 슬라이더에 다 못 오르면 나머지는 격자 맨 앞에 온다).
    const liveItems = showHero ? lives.slice(HERO_LIVES).map((live) => ({ kind: 'live', key: `l${live.id}`, live })) : []
    const videoItems = videos.items
        .map((stream) => ({ kind: 'video', key: `v${stream.id}`, stream }))
        .filter((item) => !heroIds.has(item.key) || videos.items.length <= slides.length)
    const items = [...liveItems, ...videoItems]

    return (
        <div className="home home--cinema">
            <h1 className="visually-hidden">홈</h1>

            {waking && (
                <p className="notice home__notice" role="status">
                    <Spinner size={16} />
                    서버가 쉬고 있다가 깨는 중이에요. 처음 접속하면 30초 넘게 걸릴 수 있어요. 잠시만 기다려 주세요.
                </p>
            )}

            {showHero && (
                <section className="cine-stage" aria-label="추천 방송">
                    {heroLoading && <HeroSkeleton />}

                    {livesError && !heroLoading && (
                        <p className="error cine-stage__error" role="alert">
                            방송 목록을 불러오지 못했어요. {livesError}{' '}
                            <button type="button" onClick={onRetryLives}>
                                다시 시도
                            </button>
                        </p>
                    )}

                    {!heroLoading && !livesError && slides.length > 0 && (
                        <>
                            {lives.length === 0 && <NoLiveBar />}
                            <div className="cine-stage__row">
                                <Hero slides={slides} />
                                <SchedulePanel schedules={schedules} loading={scheduleLoading} liveCount={lives.length} />
                            </div>
                        </>
                    )}

                    {noContent && (
                        <>
                            <NoLive />
                            <StartGuide />
                        </>
                    )}
                </section>
            )}

            {showHero && <SubscribedChannels />}

            {!(noContent && showHero) && (
            <section className="zone cine-bento" aria-labelledby="bento-title">
                <div className="shead">
                    <h2 id="bento-title">{showHero ? '지금 볼 만한 것' : '영상'}</h2>

                    <div className="seg" role="group" aria-label="정렬">
                        {sorts.map((sort) => (
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

                {videos.error && (
                    <p className="error" role="alert">
                        영상 목록을 불러오지 못했어요. {videos.error}{' '}
                        <button type="button" onClick={videos.retry}>
                            다시 시도
                        </button>
                    </p>
                )}

                {videos.loading && <BentoSkeleton />}

                {!videos.loading && !videos.error && (items.length > 0 || categoryId !== '') && (
                    <BentoGrid
                        items={items}
                        lives={lives}
                        popular={popular}
                        categories={categories}
                        categoryId={categoryId}
                        onCategory={onCategory}
                    />
                )}

                {!videos.loading && !videos.error && items.length === 0 && categoryId !== '' && (
                    <EmptyVideos filtered onShowAll={() => onCategory('')} />
                )}

                {!videos.loading && !videos.error && items.length === 0 && categoryId === '' && (
                    <EmptyVideos filtered={false} onShowAll={() => onCategory('')} />
                )}

                {!videos.loading && items.length > 0 && !videos.last && (
                    <div className="vmore">
                        {videos.more.error && <p className="error">{videos.more.error}</p>}
                        <button type="button" className="btn btn--lg" onClick={videos.loadMore} disabled={videos.more.loading}>
                            {videos.more.loading ? <Spinner size={18} label="불러오는 중…" /> : '영상 더 보기'}
                        </button>
                    </div>
                )}
            </section>
            )}
        </div>
    )
}

/** 방송이 있으면 방송(시청자 많은 순으로 정렬돼 들어온다), 없으면 가장 많이 본 영상을 슬라이드로 올린다. */
function buildSlides(lives, popular) {
    if (lives.length > 0) {
        return lives.slice(0, HERO_LIVES).map((live) => ({ kind: 'live', key: `l${live.id}`, live }))
    }

    return popular.slice(0, HERO_VIDEOS).map((stream) => ({ kind: 'video', key: `v${stream.id}`, stream }))
}

/* ── 시네마 슬라이더 ── */
function Hero({ slides }) {
    const [index, setIndex] = useState(0)
    const [paused, setPaused] = useState(false)
    const count = slides.length
    const current = index % count

    useEffect(() => {
        if (count < 2 || paused) return undefined
        // 움직임 줄이기를 켠 사람에게는 저절로 넘기지 않는다. 직접 넘기는 버튼은 그대로 있다.
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return undefined

        const timer = setInterval(() => setIndex((value) => (value + 1) % count), SLIDE_MS)

        return () => clearInterval(timer)
    }, [count, paused])

    const slide = slides[current]

    return (
        <div
            className="cine-hero"
            role="group"
            aria-roledescription="슬라이드"
            aria-label={`추천 ${current + 1} / ${count}`}
            onMouseEnter={() => setPaused(true)}
            onMouseLeave={() => setPaused(false)}
            onFocus={() => setPaused(true)}
            onBlur={() => setPaused(false)}
        >
            {slide.kind === 'live' ? <HeroLive key={slide.key} live={slide.live} /> : <HeroVideo key={slide.key} stream={slide.stream} />}

            {count > 1 && (
                <div className="cine-hero__nav">
                    <button
                        type="button"
                        className="cine-hero__arrow"
                        aria-label="이전 추천"
                        onClick={() => setIndex((value) => (value + count - 1) % count)}
                    >
                        <Icon name="left" size={18} />
                    </button>
                    <div className="cine-hero__dots">
                        {slides.map((item, i) => (
                            <button
                                key={item.key}
                                type="button"
                                className="cine-hero__dot"
                                aria-label={`${i + 1}번째 추천 보기`}
                                aria-current={i === current}
                                onClick={() => setIndex(i)}
                            />
                        ))}
                    </div>
                    <button
                        type="button"
                        className="cine-hero__arrow"
                        aria-label="다음 추천"
                        onClick={() => setIndex((value) => (value + 1) % count)}
                    >
                        <Icon name="right" size={18} />
                    </button>
                </div>
            )}
        </div>
    )
}

function HeroLive({ live }) {
    const now = useNow()
    const viewers = useLiveViewerCount(live.id, live.viewerCount)
    const uptime = formatLiveFor(now - toTime(live.startedAt))
    const note = AUDIENCE_NOTE[live.audience]

    return (
        <HeroFrame
            image={live.thumbnailUrl}
            seed={live.channelId}
            name={live.nickname}
            badges={
                <>
                    <span className="cine-tag cine-tag--live">LIVE</span>
                    <span className="cine-tag num">
                        <Icon name="eye" size={14} />
                        {viewers.toLocaleString('ko-KR')}명 시청 중
                    </span>
                </>
            }
            title={live.title}
            text={live.description}
            primary={{ to: { view: 'live', id: live.id }, label: '지금 보기', icon: 'play' }}
            secondary={{ to: { view: 'channel', id: live.channelId }, label: '채널 보기' }}
            who={{
                to: { view: 'channel', id: live.channelId },
                nickname: live.nickname,
                avatar: live.profileImage,
                seed: live.channelId,
                live: true,
                lines: [`${uptime} 째 방송 중`, [live.categoryName, note].filter(Boolean).join(' · ')],
            }}
        />
    )
}

function HeroVideo({ stream }) {
    const ago = timeAgo(stream.createdAt)

    return (
        <HeroFrame
            image={stream.thumbnailUrl}
            seed={stream.userId}
            name={stream.nickname}
            badges={
                <>
                    <span className="cine-tag cine-tag--pick">가장 많이 본 영상</span>
                    <span className="cine-tag num">{formatViews(stream.viewCount)}</span>
                </>
            }
            title={stream.title}
            text={stream.description}
            primary={{ to: { view: 'stream', id: stream.id }, label: '재생하기', icon: 'play' }}
            secondary={{ to: { view: 'channel', id: stream.userId }, label: '채널 보기' }}
            who={{
                to: { view: 'channel', id: stream.userId },
                nickname: stream.nickname,
                seed: stream.userId,
                lines: [ago, stream.categoryName],
            }}
            duration={stream.durationSeconds}
        />
    )
}

function HeroFrame({ image, seed, name, badges, title, text, primary, secondary, who, duration }) {
    return (
        <article className="cine-hero__slide" data-tint={tintOf(seed ?? name)}>
            <div className="cine-hero__bg" aria-hidden="true">
                {image && <img src={assetUrl(image)} alt="" decoding="async" draggable="false" />}
            </div>
            <div className="cine-hero__shade" aria-hidden="true" />

            <div className="cine-hero__content">
                <div className="cine-hero__badges">{badges}</div>
                <h2 className="cine-hero__title">{title}</h2>
                {text && <p className="cine-hero__text">{text}</p>}
                <div className="cine-hero__actions">
                    <Link to={primary.to} className="btn btn--primary btn--lg">
                        <Icon name={primary.icon} size={20} filled />
                        {primary.label}
                    </Link>
                    <Link to={secondary.to} className="btn btn--violet btn--lg">
                        {secondary.label}
                    </Link>
                </div>
            </div>

            <Link to={who.to} className="cine-hero__who">
                {image && (
                    <span className="cine-hero__who-thumb" aria-hidden="true">
                        <Thumb src={image} name={name} seed={seed} />
                        {duration ? <DurationBadge seconds={duration} /> : null}
                    </span>
                )}
                <span className="cine-hero__who-row">
                    <Avatar name={who.nickname} src={who.avatar} size={36} seed={who.seed} />
                    <span className="cine-hero__who-text">
                        <strong>{who.nickname}</strong>
                        {who.lines.filter(Boolean).map((line) => (
                            <span key={line}>{line}</span>
                        ))}
                    </span>
                </span>
            </Link>
        </article>
    )
}

/* ── 오른쪽 패널: 곧 시작하는 방송 ── */
function SchedulePanel({ schedules, loading, liveCount }) {
    const upcoming = schedules.slice(0, 4)

    return (
        <aside className="cine-panel" aria-labelledby="panel-title">
            <p className="cine-panel__now">
                <span className={`cine-dot${liveCount > 0 ? ' cine-dot--on' : ''}`} aria-hidden="true" />
                {liveCount > 0 ? `지금 ${liveCount}개 채널이 방송 중` : '지금 방송 중인 채널이 없어요'}
            </p>

            <div className="cine-panel__head">
                <h2 id="panel-title">곧 시작하는 방송</h2>
                <Link to={{ view: 'lives' }} className="cine-link">
                    전체
                </Link>
            </div>

            {loading && (
                <ul className="cine-panel__list" aria-hidden="true">
                    {[0, 1, 2].map((i) => (
                        <li key={i} className="cine-skel cine-skel--row" />
                    ))}
                </ul>
            )}

            {!loading && upcoming.length > 0 && (
                <ul className="cine-panel__list">
                    {upcoming.map((schedule) => (
                        <li key={schedule.id}>
                            <Link to={{ view: 'schedule', id: schedule.id }} className="cine-sched">
                                <span className="cine-chip num">{formatWhen(schedule.scheduledAt)}</span>
                                <strong>{schedule.title}</strong>
                                <span>{schedule.nickname}</span>
                            </Link>
                        </li>
                    ))}
                </ul>
            )}

            {!loading && upcoming.length === 0 && (
                <p className="cine-panel__empty">예정된 방송이 아직 없어요. 방송이 예약되면 시작 시각과 함께 여기에 올라와요.</p>
            )}
        </aside>
    )
}

/* ── 벤토 격자 ── */

function BentoGrid({ items, lives, popular, categories, categoryId, onCategory }) {
    const streamers = topStreamers(lives, items, popular)
    const head = items.slice(0, 3)
    const tail = items.slice(3)

    return (
        <ul className="bento">
            {head.map((item, i) => (
                <BentoItem key={item.key} item={item} size={tileSize(i)} index={i} />
            ))}

            {streamers.length > 0 && (
                <li className="bento__item bento__item--module" style={{ '--i': head.length }}>
                    <StreamersModule streamers={streamers} />
                </li>
            )}

            <li className="bento__item bento__item--module" style={{ '--i': head.length + 1 }}>
                <GenresModule categories={categories} categoryId={categoryId} onCategory={onCategory} />
            </li>

            {tail.map((item, i) => (
                <BentoItem key={item.key} item={item} size={tileSize(i + 3)} index={head.length + 2 + i} />
            ))}
        </ul>
    )
}

/** 방송 중인 채널 먼저(시청자 많은 순), 그다음 불러온 영상의 조회수 합이 큰 채널. 화면에 있는 것만으로 센다. */
function topStreamers(lives, items, popular) {
    const channels = new Map()

    function add(id, nickname, avatar, score, live) {
        const prev = channels.get(id) ?? { id, nickname, avatar, score: 0, live: false }
        channels.set(id, { ...prev, avatar: prev.avatar ?? avatar, score: prev.score + score, live: prev.live || live })
    }

    lives.forEach((live) => add(live.channelId, live.nickname, live.profileImage, live.viewerCount ?? 0, true))

    const seen = new Set()
    ;[...items.filter((item) => item.kind === 'video').map((item) => item.stream), ...popular].forEach((stream) => {
        if (seen.has(stream.id)) return
        seen.add(stream.id)
        add(stream.userId, stream.nickname, undefined, stream.viewCount ?? 0, false)
    })

    return [...channels.values()]
        .sort((a, b) => Number(b.live) - Number(a.live) || b.score - a.score)
        .slice(0, 5)
}

function StreamersModule({ streamers }) {
    return (
        <section className="bmod bmod--green" aria-labelledby="streamers-title">
            <header className="bmod__head">
                <h3 id="streamers-title">주목받는 채널</h3>
                <span>방송 중 먼저, 그다음 조회수 순</span>
            </header>
            <ul className="streamers">
                {streamers.map((channel, i) => (
                    <li key={channel.id}>
                        <Link to={{ view: 'channel', id: channel.id }} className="streamer">
                            <span className={`streamer__ring${i % 2 === 1 ? ' streamer__ring--violet' : ''}`}>
                                <Avatar name={channel.nickname} src={channel.avatar} size={52} seed={channel.id} />
                            </span>
                            <span className="streamer__name">{channel.nickname}</span>
                            {channel.live && <span className="streamer__live">LIVE</span>}
                        </Link>
                    </li>
                ))}
            </ul>
        </section>
    )
}

function GenresModule({ categories, categoryId, onCategory }) {
    return (
        <section className="bmod bmod--violet" aria-labelledby="genres-title">
            <header className="bmod__head">
                <h3 id="genres-title">장르</h3>
                <span>골라서 보기</span>
            </header>
            <div className="genres" role="group" aria-label="카테고리">
                <button type="button" className="cine-chip cine-chip--btn" aria-pressed={categoryId === ''} onClick={() => onCategory('')}>
                    전체
                </button>
                {categories?.map((category) => (
                    <button
                        key={category.id}
                        type="button"
                        className="cine-chip cine-chip--btn"
                        aria-pressed={categoryId === String(category.id)}
                        onClick={() => onCategory(String(category.id))}
                    >
                        {category.name}
                    </button>
                ))}
            </div>
        </section>
    )
}

/* ── 불러오는 동안: 실제 화면과 같은 모양의 뼈대 ── */
function HeroSkeleton() {
    return (
        <div className="cine-stage__row" aria-hidden="true">
            <div className="cine-hero cine-skel cine-skel--hero" />
            <div className="cine-panel">
                <span className="cine-skel cine-skel--line" />
                <ul className="cine-panel__list">
                    {[0, 1, 2].map((i) => (
                        <li key={i} className="cine-skel cine-skel--row" />
                    ))}
                </ul>
            </div>
        </div>
    )
}

const SKELETON_TILES = ['big', 'wide', 'wide', 'module', 'module', ...Array.from({ length: 8 }, () => 'mini')]

function BentoSkeleton() {
    return (
        <ul className="bento" aria-label="목록을 불러오는 중">
            {SKELETON_TILES.map((size, i) => (
                <li key={i} className={`bento__item bento__item--${size}`} style={{ '--i': i }} aria-hidden="true">
                    <div className="cine-skel cine-skel--tile" />
                </li>
            ))}
        </ul>
    )
}
