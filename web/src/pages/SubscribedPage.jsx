import { useState } from 'react'
import { getSubscribedFeed } from '../api.js'
import Pager from '../components/Pager.jsx'
import StreamList from '../components/StreamList.jsx'
import { useAsyncData } from '../useAsyncData.js'

export default function SubscribedPage() {
    const [pageNumber, setPageNumber] = useState(0)

    const {
        data: page,
        error,
        loading,
    } = useAsyncData(() => getSubscribedFeed(pageNumber), [pageNumber])

    return (
        <section>
            <h2>구독 피드</h2>

            {error && <p className="error">{error}</p>}
            {loading && <p className="empty">불러오는 중…</p>}

            {!loading && (
                <StreamList
                    streams={page?.content}
                    empty="구독 중인 채널의 영상이 없습니다."
                />
            )}

            <Pager page={page} onChange={setPageNumber} />
        </section>
    )
}
