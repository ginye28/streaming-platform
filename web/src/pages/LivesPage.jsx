import { useState } from 'react'
import { getLives } from '../api.js'
import Pager from '../components/Pager.jsx'
import { assetUrl } from '../assets.js'
import Link from '../components/Link.jsx'
import { useAsyncData } from '../useAsyncData.js'

export default function LivesPage() {
    const [pageNumber, setPageNumber] = useState(0)

    const { data: page, error, loading } = useAsyncData(() => getLives(pageNumber), [pageNumber])

    return (
        <section>
            <h2>지금 방송 중</h2>

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
                                <img src={assetUrl(live.thumbnailUrl)} alt="" />
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

                            <p className="card__meta">시청자 {live.viewerCount}명</p>
                        </div>
                    </li>
                ))}
            </ul>

            <Pager page={page} onChange={setPageNumber} />
        </section>
    )
}
