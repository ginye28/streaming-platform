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

    return (
        <section className="home">
            {lives?.content.length > 0 && (
                <HeroBanner live={lives.content[0]} others={lives.content.slice(1)} />
            )}

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

// 오른쪽 바퀴에 놓인 작은 카드 한 장의 크기.
const CARD_W = 200
const CARD_H = 118
// 카드들이 도는 원의 반지름. 원의 중심은 배너 바깥(오른쪽)에 있어서,
// 화면에는 그 원의 왼쪽 자락만 세로로 휘어 보인다.
const RING_RADIUS = 230
// 바퀴가 차지하는 폭과, 맨 앞 카드 중심이 그 오른쪽 끝에서 얼마나 안쪽인지.
const WHEEL_W = 330
const FRONT_INSET = 120
// 카드 사이의 각도. 원 둘레를 이 간격으로 나눠 자리를 잡는다.
const SLOT_DEG = 30
// 이 각도를 넘어가면 원의 뒤쪽으로 돌아간 것이라 그리지 않는다.
const VISIBLE_DEG = 92
// 드래그 픽셀을 각도로 바꾸는 비율.
const DEG_PER_PX = 0.32

/**
 * 큰 배너는 고정이고, 그 위 오른쪽에서 작은 카드들만 원을 그리며 돈다.
 *
 * 원의 중심이 배너 바깥(오른쪽)에 있어서 화면에는 원의 왼쪽 자락만 보인다.
 * 그래서 카드들이 오른쪽 가장자리를 따라 세로로 휘어 늘어서고, 가장 왼쪽에
 * 있는(=배너 쪽으로 가장 나온) 카드가 맨 앞이 된다.
 *
 * 오른쪽에서 왼쪽으로 끌면 바퀴가 돌아 맨 앞 카드가 위로 빠지고, 아래에서
 * 다음 카드가 올라와 맨 앞이 된다. 자리는 각도를 카드 수로 나눈 나머지로
 * 정하므로 끝까지 끌어도 처음 카드로 돌아온다.
 */
function LiveWheel({ items }) {
    const ringRef = useRef(null)
    const dragRef = useRef(null)
    const [height, setHeight] = useState(420)
    const [angle, setAngle] = useState(0)
    const [dragging, setDragging] = useState(false)

    useEffect(() => {
        const el = ringRef.current
        if (!el) return

        const observer = new ResizeObserver(([entry]) => setHeight(entry.contentRect.height))
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

    // 원의 중심. 배너 오른쪽 바깥에 두어 왼쪽 자락만 보이게 한다.
    // 맨 앞 카드(phi = 0)가 오른쪽 끝에서 FRONT_INSET 만큼 안쪽에 서도록 맞춘다.
    const centerX = WHEEL_W - FRONT_INSET + RING_RADIUS
    const centerY = height / 2

    const cards = []
    const slots = Math.ceil(VISIBLE_DEG / SLOT_DEG)

    for (let k = -slots; k <= slots; k++) {
        // 이 자리가 맨 앞(0도)에서 얼마나 돌아가 있는지.
        const phi = angle - Math.round(angle / SLOT_DEG) * SLOT_DEG + k * SLOT_DEG
        if (Math.abs(phi) > VISIBLE_DEG) continue

        const rad = (phi * Math.PI) / 180
        // 원의 왼쪽 자락. phi 가 0 이면 가장 왼쪽(=맨 앞)에 선다.
        const x = centerX - RING_RADIUS * Math.cos(rad)
        const y = centerY - RING_RADIUS * Math.sin(rad)

        // 뒤로 돌아갈수록 작아지고 흐려진다.
        const depth = Math.cos(rad)
        const scale = 0.55 + 0.45 * depth
        const opacity = Math.min(1, Math.max(0, depth * 1.5))

        const index = Math.round(angle / SLOT_DEG) + k
        const item = items[((index % n) + n) % n]

        cards.push(
            <Link
                key={k}
                to={{ view: 'live', id: item.id }}
                className={`wheel__card${Math.abs(phi) < SLOT_DEG / 2 ? ' wheel__card--front' : ''}`}
                draggable="false"
                style={{
                    width: CARD_W,
                    height: CARD_H,
                    opacity,
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
            {cards}
        </div>
    )
}

/** 고정된 큰 배너. 여기는 돌지 않는다. */
function HeroBanner({ live, others }) {
    return (
        <div className="hero-wrap">
            {live.thumbnailUrl && (
                <img className="hero-bg" src={assetUrl(live.thumbnailUrl)} alt="" draggable="false" />
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

                <span className="hero-cta">▶ 지금 보기</span>
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
