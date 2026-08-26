import { useState } from 'react'
import { getCategories, getStreams } from '../api.js'
import Pager from '../components/Pager.jsx'
import StreamList from '../components/StreamList.jsx'
import { useAsyncData } from '../useAsyncData.js'

const SORTS = [
    { value: 'LATEST', label: '최신순' },
    { value: 'POPULAR', label: '인기순' },
]

export default function HomePage() {
    const [categoryId, setCategoryId] = useState('')
    const [sortBy, setSortBy] = useState('LATEST')
    const [pageNumber, setPageNumber] = useState(0)

    const { data: categories } = useAsyncData(getCategories, [])

    const {
        data: page,
        error,
        loading,
    } = useAsyncData(
        () => getStreams(categoryId, sortBy, pageNumber),
        [categoryId, sortBy, pageNumber]
    )

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

                <label htmlFor="home-sort">정렬</label>

                <select
                    id="home-sort"
                    value={sortBy}
                    onChange={(e) => {
                        setSortBy(e.target.value)
                        setPageNumber(0)
                    }}
                >
                    {SORTS.map((sort) => (
                        <option key={sort.value} value={sort.value}>
                            {sort.label}
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
