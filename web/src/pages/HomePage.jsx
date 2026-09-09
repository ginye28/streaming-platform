import { useRef, useState } from 'react'
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
            {featured && <Hero live={featured} next={rest.slice(0, 4)} />}

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

            {next.length > 0 && (
                <ul className="hero__rail">
                    {next.map((item) => (
                        <li key={item.id}>
                            <Link to={{ view: 'live', id: item.id }} className="hero__rail-item">
                                {item.thumbnailUrl ? (
                                    <img src={assetUrl(item.thumbnailUrl)} alt="" />
                                ) : (
                                    <span className="hero__rail-blank">LIVE</span>
                                )}
                                <span className="hero__rail-title">{item.title}</span>
                            </Link>
                        </li>
                    ))}
                </ul>
            )}
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
                        <Link to={{ view: 'stream', id: stream.id }} className="tile">
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
