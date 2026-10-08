import { useState } from 'react'
import { getLives, getSchedules } from '../api.js'
import Pager from '../components/Pager.jsx'
import { assetUrl } from '../assets.js'
import Link from '../components/Link.jsx'
import ScheduleList from '../components/ScheduleList.jsx'
import { useAsyncData } from '../useAsyncData.js'

export default function LivesPage() {
    const [pageNumber, setPageNumber] = useState(0)

    const { data: page, error, loading } = useAsyncData(() => getLives(pageNumber), [pageNumber])

    const { data: schedules } = useAsyncData(() => getSchedules(0), [])

    return (
        <section>
            {schedules && (
                <>
                    <h2>방송 예정</h2>
                    <ScheduleList schedules={schedules.content} />
                </>
            )}

            <h2>지금 방송 중</h2>

            <p className="meta">
                <Link to={{ view: 'broadcast' }}>브라우저로 바로 방송하기</Link> — OBS 없이 화면 공유나 카메라로 방송해요.
            </p>

            {error && <p className="error">{error}</p>}
            {loading && <p className="empty">불러오는 중…</p>}

            {!loading && page?.content.length === 0 && (
                <p className="empty">진행 중인 방송이 없습니다.</p>
            )}

            <ul className="cards">
                {page?.content.map((live) => (
                    <li key={live.id} className="card">
                        <Link to={{ view: 'live', id: live.id }} className="card__thumb">
                            {live.thumbnailUrl ? (
                                <img src={assetUrl(live.thumbnailUrl)} alt="" loading="lazy" decoding="async" />
                            ) : (
                                <div className="card__thumb--blank">LIVE</div>
                            )}
                        </Link>

                        <div className="card__body">
                            <Link to={{ view: 'live', id: live.id }} className="card__title">
                                {live.title}
                            </Link>

                            <Link
                                to={{ view: 'channel', id: live.channelId }}
                                className="card__meta"
                            >
                                {live.nickname}
                            </Link>

                            <p className="card__meta">
                                시청자 {live.viewerCount}명
                                {live.audience === 'SUBSCRIBERS' && ' · 구독자 전용'}
                                {live.audience === 'PAID' && ' · 유료 구독자 전용'}
                            </p>
                        </div>
                    </li>
                ))}
            </ul>

            <Pager page={page} onChange={setPageNumber} />
        </section>
    )
}
