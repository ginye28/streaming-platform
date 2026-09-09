import { useEffect, useRef, useState } from 'react'
import { getCategories, getLives, getStreams } from '../api.js'
import { assetUrl } from '../assets.js'
import Link from '../components/Link.jsx'
import StreamList from '../components/StreamList.jsx'
import { useAsyncData } from '../useAsyncData.js'

const SORTS = [
    { value: 'LATEST', label: '최신순' },
    { value: 'POPULAR', label: '인기순' },
]

export default function HomePage() {
    const [categoryId, setCategoryId] = useState('')
    const [sortBy, setSortBy] = useState('LATEST')

    const { data: categories } = useAsyncData(getCategories, [])
    const { data: lives } = useAsyncData(() => getLives(0), [])

    const {
        data: page,
        error,
        loading,
    } = useAsyncData(() => getStreams(categoryId, sortBy, 0), [categoryId, sortBy])

    // 맨 앞 방송을 큰 배너로 세우고, 나머지는 옆에 걸쳐 둔다.
    const [featured, ...rest] = lives?.content ?? []

    return (
        <section className="home">
            {featured && <Hero live={featured} next={rest.slice(0, 10)} />}

            <div className="home__filters">
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

                <span className="home__filters-gap" />

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

            {error && <p className="error">{error}</p>}
            {loading && <p className="empty">불러오는 중…</p>}

            {!loading && page?.content.length > 0 && (
                <Carousel title="새로 올라온 영상" items={page.content} />
            )}

            {!loading && page?.content.length === 0 && <StreamList streams={[]} />}
        </section>
    )
}

function Hero({ live, next }) {
    return (
        <div className="hero">
            <Link to={{ view: 'live', id: live.id }} className="hero__main">
                {live.thumbnailUrl && (
                    <img className="hero__bg" src={assetUrl(live.thumbnailUrl)} alt="" />
                )}

                <div className="hero__body">
                    <div className="hero__badges">
                        <span className="pill pill--live">LIVE</span>
                        <span className="pill">시청자 {formatCount(live.viewerCount)}</span>
                    </div>

                    <h2 className="hero__title">{live.title}</h2>

                    <span className="hero__channel">
                        <span className="hero__avatar" aria-hidden="true">
                            {live.nickname?.slice(0, 1)}
                        </span>
                        {live.nickname}
                    </span>

                    <span className="hero__cta">▶ 지금 보기</span>
                </div>
            </Link>

            {next.length > 0 && <RailWheel items={next} />}
        </div>
    )
}

/**
 * 오른쪽에 곡선을 그리며 걸쳐 있는 카드들. 세로로 끌면 원을 그리듯 돌아가며
 * 다음 카드가 앞으로(왼쪽·정중앙 쪽으로) 나온다 — 끝까지 끌어도 처음으로
 * 되돌아오는 무한 루프다. 정중앙에서 멀어질수록 오른쪽으로 휘어지며 작아지다가
 * 컨테이너 밖으로 잘려 나간다 (overflow: hidden).
 */
function RailWheel({ items }) {
    const containerRef = useRef(null)
    const dragRef = useRef(null)
    const [containerHeight, setContainerHeight] = useState(340)
    const [offset, setOffset] = useState(0)
    const [dragging, setDragging] = useState(false)

    useEffect(() => {
        const el = containerRef.current
        if (!el) return

        const observer = new ResizeObserver(([entry]) => setContainerHeight(entry.contentRect.height))
        observer.observe(el)
        return () => observer.disconnect()
    }, [])

    const CARD_HEIGHT = 116
    const SPACING = 132
    const n = items.length
    const loopHeight = n * SPACING

    function wrap(value) {
        if (loopHeight === 0) return 0
        return ((value % loopHeight) + loopHeight) % loopHeight
    }

    function handlePointerDown(event) {
        dragRef.current = { startY: event.clientY, startOffset: offset, moved: false }
        setDragging(true)
        containerRef.current?.setPointerCapture(event.pointerId)
    }

    function handlePointerMove(event) {
        const drag = dragRef.current
        if (!drag) return

        const delta = event.clientY - drag.startY
        // 살짝 흔들린 것까지 드래그로 치면 클릭이 죽는다.
        if (Math.abs(delta) > 4) drag.moved = true
        setOffset(wrap(drag.startOffset - delta))
    }

    function handlePointerUp(event) {
        containerRef.current?.releasePointerCapture(event.pointerId)
        setDragging(false)
        // 놓으면 가장 가까운 카드가 정중앙으로 딱 맞게 스냅한다.
        setOffset((current) => wrap(Math.round(current / SPACING) * SPACING))
        dragRef.current = null
    }

    // 끌어서 넘긴 직후의 클릭은 이동시키지 않는다.
    function handleClickCapture(event) {
        if (dragRef.current?.moved) {
            event.preventDefault()
            event.stopPropagation()
        }
    }

    const centerY = containerHeight / 2
    const cards = []

    if (n > 0) {
        // 화면 위아래로 한 칸씩 여유를 두고, 그 범위에 걸리는 슬롯만 그린다.
        const from = Math.floor((offset - SPACING - centerY) / SPACING) - 1
        const to = Math.ceil((offset + containerHeight + SPACING - centerY) / SPACING) + 1

        for (let slot = from; slot <= to; slot++) {
            const y = centerY + slot * SPACING - offset
            if (y < -SPACING || y > containerHeight + SPACING) continue

            // 정중앙에서 멀어진 정도(0~1). 멀수록 오른쪽으로 휘고 작아지고 흐려진다.
            const t = Math.min(Math.abs(y - centerY) / (centerY || 1), 1)
            const bulge = 46 * t ** 1.6
            const scale = 1 - 0.22 * t
            const opacity = 1 - 0.5 * t
            const item = items[((slot % n) + n) % n]

            cards.push(
                <Link
                    key={slot}
                    to={{ view: 'live', id: item.id }}
                    className="railwheel__card"
                    draggable="false"
                    style={{
                        transform: `translate(${bulge}px, ${y - CARD_HEIGHT / 2}px) scale(${scale})`,
                        opacity,
                        zIndex: Math.round((1 - t) * 100),
                        transition: dragging ? 'none' : 'transform 320ms ease, opacity 320ms ease',
                    }}
                >
                    {item.thumbnailUrl ? (
                        <img src={assetUrl(item.thumbnailUrl)} alt="" draggable="false" />
                    ) : (
                        <span className="railwheel__blank">LIVE</span>
                    )}
                    <span className="railwheel__title">{item.title}</span>
                </Link>
            )
        }
    }

    return (
        <div
            className="hero__rail"
            ref={containerRef}
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={handlePointerUp}
            onPointerCancel={handlePointerUp}
            onClickCapture={handleClickCapture}
        >
            {cards}
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
            <h3 className="carousel__title">{title}</h3>

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
                                    <img src={assetUrl(stream.thumbnailUrl)} alt="" draggable="false" />
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
        <button type="button" className={`chip${active ? ' chip--on' : ''}`} onClick={onClick}>
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
