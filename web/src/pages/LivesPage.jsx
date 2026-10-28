import { useState } from 'react'
import { getLives, getSchedules } from '../api.js'
import Icon from '../components/Icon.jsx'
import Link from '../components/Link.jsx'
import { LiveCard } from '../components/MediaCards.jsx'
import Pager from '../components/Pager.jsx'
import ScheduleList from '../components/ScheduleList.jsx'
import { useAsyncData } from '../useAsyncData.js'

export default function LivesPage() {
    const [pageNumber, setPageNumber] = useState(0)

    const { data: page, error, loading } = useAsyncData(() => getLives(pageNumber), [pageNumber])

    const { data: schedules } = useAsyncData(() => getSchedules(0), [])

    return (
        <section className="listpage">
            {schedules && schedules.content.length > 0 && (
                <>
                    <header className="page-head">
                        <h2>방송 예정</h2>
                    </header>
                    <ScheduleList schedules={schedules.content} />
                </>
            )}

            <header className="page-head">
                <h2>
                    지금 방송 중
                    {page && page.totalElements > 0 && <span className="page-head__count num">{page.totalElements}</span>}
                </h2>
                <Link to={{ view: 'broadcast' }} className="btn btn--tonal">
                    <Icon name="live" size={18} />
                    브라우저로 바로 방송하기
                </Link>
            </header>

            {error && <p className="error">{error}</p>}
            {loading && <p className="empty">불러오는 중…</p>}

            {!loading && page?.content.length === 0 && (
                <p className="empty">진행 중인 방송이 없습니다. OBS 없이도 화면 공유나 카메라로 바로 방송할 수 있어요.</p>
            )}

            {page?.content.length > 0 && (
                <ul className="vgrid vgrid--live" aria-label="방송 목록">
                    {page.content.map((live, i) => (
                        <LiveCard key={live.id} live={live} index={i} />
                    ))}
                </ul>
            )}

            <Pager page={page} onChange={setPageNumber} />
        </section>
    )
}
