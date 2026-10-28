import { useState } from 'react'
import { searchStreams } from '../api.js'
import Pager from '../components/Pager.jsx'
import StreamList from '../components/StreamList.jsx'
import { useAsyncData } from '../useAsyncData.js'

export default function SearchPage({ keyword }) {
    const [pageNumber, setPageNumber] = useState(0)

    const {
        data: page,
        error,
        loading,
    } = useAsyncData(
        () => (keyword ? searchStreams(keyword, pageNumber) : Promise.resolve(null)),
        [keyword, pageNumber]
    )

    return (
        <section>
            <h2>&quot;{keyword}&quot; 검색 결과</h2>

            {error && <p className="error">{error}</p>}
            {loading && <p className="empty">불러오는 중…</p>}

            {!loading && (
                <StreamList streams={page?.content} empty="검색 결과가 없습니다." />
            )}

            <Pager page={page} onChange={setPageNumber} />
        </section>
    )
}
