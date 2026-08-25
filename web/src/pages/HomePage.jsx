import { useState } from 'react'
import { getCategories, getStreams } from '../api.js'
import Pager from '../components/Pager.jsx'
import StreamList from '../components/StreamList.jsx'
import { useAsyncData } from '../useAsyncData.js'

export default function HomePage() {
    const [categoryId, setCategoryId] = useState('')
    const [pageNumber, setPageNumber] = useState(0)

    const { data: categories } = useAsyncData(getCategories, [])

    const {
        data: page,
        error,
        loading,
    } = useAsyncData(() => getStreams(categoryId, pageNumber), [categoryId, pageNumber])

    return (
        <section>
            <div className="toolbar">
                <label htmlFor="home-category">카테고리</label>

                <select
                    id="home-category"
                    value={categoryId}
                    onChange={(e) => {
                        setCategoryId(e.target.value)
                        setPageNumber(0)
                    }}
                >
                    <option value="">전체</option>
                    {categories?.map((category) => (
                        <option key={category.id} value={category.id}>
                            {category.name}
                        </option>
                    ))}
                </select>
            </div>

            {error && <p className="error">{error}</p>}
            {loading && <p className="empty">불러오는 중…</p>}

            {!loading && <StreamList streams={page?.content} />}

            <Pager page={page} onChange={setPageNumber} />
        </section>
    )
}
