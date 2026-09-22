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

// 맨 앞(가장 아래)까지 내려온 카드 한 장의 크기. 뒤로 갈수록 이보다 작아진다.
const CARD_W = 184
const CARD_H = 112
// 카드가 도는 타원. 중심은 배너 안쪽 오른편에 있고, 눕혀진 원이라
// 가로가 세로보다 넓다 — 위에서 비스듬히 내려다본 회전목마 모양이다.
const RING_CX = 0.71
const RING_CY = 0.46
const RING_RX = 0.22
const RING_RY = 0.30
// 카드 사이의 각도. 보이는 반 바퀴(180도)에 여섯 장쯤 놓인다.
const SLOT_DEG = 36
// 맨 앞 카드가 서는 각도. 90 이 타원의 맨 아래다.
const FRONT_DEG = 90
// 이 각도 바깥은 타원의 뒤쪽(왼쪽 절반)이라 글자 뒤로 숨는다.
const FADE_IN_DEG = -100
const SOLID_FROM_DEG = -84
const SOLID_TO_DEG = 88
const FADE_OUT_DEG = 104
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
