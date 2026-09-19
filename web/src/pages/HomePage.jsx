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
            {lives?.content.length > 0 && <HeroWheel items={lives.content} />}

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

// 한 칸 뒤로 물러난 카드가 줄어들 너비. 맨 앞 카드는 배너를 꽉 채우고,
// 뒤로 갈수록 이 크기까지 줄며 오른쪽 아래로 흘러간다.
const PEEK_WIDTH = 300
// 한 칸 물러날 때마다 더 기우는 각도.
const TILT_PER_STEP = 24
// 한 칸 물러날 때마다 앞으로 띄우는 깊이. 뒤 카드가 앞 카드 위에 겹쳐 보이게 한다.
const LIFT_PER_STEP = 26
// 드래그 픽셀을 각도로 바꾸는 비율.
const DEG_PER_PX = 0.35
// 이 칸수보다 더 뒤로 물러난 카드는 그리지 않는다.
const PEEK_LIMIT = 3.4

/**
 * 지금 방송 중인 채널들이 구를 이루며 돈다. 맨 앞의 것 하나만 온전한 배너로
 * 꽉 채워 보이고, 나머지는 오른쪽에서 휘어져 들어오며 다가온다.
 * 오른쪽에서 왼쪽으로 끌면 맨 앞 카드가 뒤로 돌아 사라지고, 오른쪽에 걸쳐
 * 있던 다음 카드가 돌아 들어와 새로 맨 앞이 된다 — 끝까지 끌어도 처음으로
 * 돌아오는 무한 루프다.
 */
function HeroWheel({ items }) {
    const wrapRef = useRef(null)
    const dragRef = useRef(null)
    const [size, setSize] = useState({ width: 0, height: 420 })
    const [angle, setAngle] = useState(0)
    const [dragging, setDragging] = useState(false)

    useEffect(() => {
        const el = wrapRef.current
        if (!el) return

        const observer = new ResizeObserver(([entry]) => {
            setSize({ width: entry.contentRect.width, height: entry.contentRect.height })
        })
        observer.observe(el)
        return () => observer.disconnect()
    }, [])

    const n = items.length
    const step = 360 / n

    function handlePointerDown(event) {
        dragRef.current = { startX: event.clientX, startAngle: angle, moved: false }
        setDragging(true)
        wrapRef.current?.setPointerCapture(event.pointerId)
    }

    function handlePointerMove(event) {
        const drag = dragRef.current
        if (!drag) return

        const delta = event.clientX - drag.startX
        // 살짝 흔들린 것까지 드래그로 치면 클릭이 죽는다.
        if (Math.abs(delta) > 4) drag.moved = true
        // 오른쪽 → 왼쪽 드래그(delta 음수)일수록 각도가 커져서, 오른쪽에
        // 걸쳐 있던 다음 카드가 정면(0도)으로 끌려온다.
        setAngle(drag.startAngle - delta * DEG_PER_PX)
    }

    function handlePointerUp(event) {
        wrapRef.current?.releasePointerCapture(event.pointerId)
        setDragging(false)
        // 손을 놓으면 가장 가까운 카드가 정확히 정면에 오도록 스냅한다.
        setAngle((current) => Math.round(current / step) * step)
        dragRef.current = null
    }

    // 끌어서 넘긴 직후의 클릭은 이동시키지 않는다.
    function handleClickCapture(event) {
        if (dragRef.current?.moved) {
            event.preventDefault()
            event.stopPropagation()
        }
    }

    const { width: W, height: H } = size

    // 한 칸 물러난 카드가 놓일 자리와, 그보다 더 뒤로 갈 때마다 밀려나는 양.
    const peekScale = W > 0 ? PEEK_WIDTH / W : 0
    const peekX = W - PEEK_WIDTH * 0.22
    const peekY = H * 0.23
    const stepX = PEEK_WIDTH * 0.3
    const stepY = H * 0.22

    // 아직 그릇 크기를 재기 전에는 배치를 계산할 수 없다. 한 프레임 뒤에 그린다.
    const cards = W === 0 ? [] : items.map((item, i) => {
        // 이 카드가 지금 정면에서 몇 도 떨어져 있는지, -180~180 사이로 접어 넣는다.
        // 양수면 아직 오지 않은(오른쪽에 걸쳐 있는) 카드, 음수면 막 지나간 카드다.
        const fromFront = ((i * step - angle + 540) % 360) - 180
        // 각도 대신 "몇 칸 뒤인지"로 보면 화면 배치를 계산하기 쉽다.
        const back = fromFront / step

        // 아직 올 카드는 오른쪽에, 막 지나간 카드는 왼쪽에 둔다. 가는 길은
        // 좌우 대칭이라 방향만 뒤집어 같은 식을 쓴다.
        const away = Math.abs(back)
        const side = back >= 0 ? 1 : -1

        // 지나간 카드는 한 칸을 다 가기 전에 사라지고, 너무 뒤엣것은 아예 안 그린다.
        // 그래야 멈춰 있을 때 스케치처럼 오른쪽에만 카드가 걸쳐 보인다.
        if (back <= -1 || back > PEEK_LIMIT) return null

        const isFront = away < 0.5
        // 한 칸까지는 배너 크기에서 작은 카드 크기로 이어지듯 줄고,
        // 그 뒤로는 조금씩만 더 줄며 바깥 아래로 흘러간다.
        const t = Math.min(away, 1)
        const beyond = Math.max(away - 1, 0)

        const scale = (1 + (peekScale - 1) * t) * (1 - beyond * 0.12)
        const centerX = W / 2 + side * ((peekX - W / 2) * t + beyond * stepX)
        const centerY = H / 2 + (peekY - H / 2) * t + beyond * stepY

        // 넘어가는 동안에는 지나가는 카드와 들어오는 카드가 겹쳐 화면을 채워야
        // 한다. 그래서 지나간 카드는 거의 다 갈 때까지 불투명하게 두고 막판에만 지운다.
        const opacity =
            back < 0
                ? Math.min(1, Math.max(0, (1 - away) / 0.25))
                : Math.min(1, Math.max(0, (PEEK_LIMIT - back) / 1))

        return (
            <div
                key={item.id}
                className={`hero-card${isFront ? ' hero-card--front' : ''}`}
                style={{
                    opacity,
                    // 뒤로 갈수록 위에 겹치게 둔다. 스케치처럼 앞 카드의 오른쪽
                    // 모서리를 타고 넘어가는 모양이 된다.
                    zIndex: Math.round(500 + back * 10),
                    transform: [
                        `translate(${centerX - W / 2}px, ${centerY - H / 2}px)`,
                        `translateZ(${side * Math.min(away, PEEK_LIMIT) * LIFT_PER_STEP}px)`,
                        `rotateY(${side * Math.min(away, PEEK_LIMIT) * TILT_PER_STEP}deg)`,
                        `scale(${scale})`,
                    ].join(' '),
                    transition: dragging ? 'none' : 'transform 380ms ease, opacity 380ms ease',
                }}
            >
                {item.thumbnailUrl && (
                    <img className="hero-card__bg" src={assetUrl(item.thumbnailUrl)} alt="" draggable="false" />
                )}

                {isFront ? (
                    <Link to={{ view: 'live', id: item.id }} className="hero-card__link" draggable="false">
                        <div className="hero-card__body">
                            <div className="hero-card__badges">
                                <span className="pill pill--live">LIVE</span>
                                <span className="pill">시청자 {formatCount(item.viewerCount)}</span>
                            </div>

                            <h2 className="hero-card__title">{item.title}</h2>

                            <span className="hero-card__channel">
                                <span className="hero-card__avatar" aria-hidden="true">
                                    {item.nickname?.slice(0, 1)}
                                </span>
                                {item.nickname}
                            </span>

                            <span className="hero-card__cta">▶ 지금 보기</span>
                        </div>
                    </Link>
                ) : (
                    <span
                        className="hero-card__peek-title"
                        // 카드 전체가 scale 로 줄어드니, 글자는 그만큼 키워야 원래 크기로 보인다.
                        style={{ fontSize: 15 / scale, padding: `${40 / scale}px ${20 / scale}px ${16 / scale}px` }}
                    >
                        {item.title}
                    </span>
                )}
            </div>
        )
    })

    return (
        <div
            className="hero-wrap"
            ref={wrapRef}
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={handlePointerUp}
            onPointerCancel={handlePointerUp}
            onClickCapture={handleClickCapture}
        >
            <div className="hero-stage">{cards}</div>
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
