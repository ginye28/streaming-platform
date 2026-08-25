import { useState } from 'react'
import { createCategory, getCategories } from '../api.js'
import { useAsyncData } from './useAsyncData.js'

export default function CategoriesTab() {
    const [name, setName] = useState('')
    const [busy, setBusy] = useState(false)

    const { data: categories, error, loading, reload, fail } = useAsyncData(getCategories, [])

    async function handleCreate(event) {
        event.preventDefault()
        setBusy(true)

        try {
            await createCategory(name.trim())
            setName('')
            reload()
        } catch (e) {
            fail(e)
        } finally {
            setBusy(false)
        }
    }

    return (
        <div className="admin__panel">
            <form className="admin__toolbar" onSubmit={handleCreate}>
                <input
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="새 카테고리 이름"
                    maxLength={30}
                    required
                />

                <button className="admin__button--primary" type="submit" disabled={busy}>
                    {busy ? '추가 중…' : '추가'}
                </button>
            </form>

            {error && <p className="admin__error">{error}</p>}

            {loading && <p className="admin__empty">불러오는 중…</p>}

            {!loading && categories?.length === 0 && (
                <p className="admin__empty">아직 카테고리가 없습니다.</p>
            )}

            {!loading && categories?.length > 0 && (
                <div className="admin__scroll">
                    <table>
                        <thead>
                            <tr>
                                <th>번호</th>
                                <th>이름</th>
                            </tr>
                        </thead>

                        <tbody>
                            {categories.map((category) => (
                                <tr key={category.id}>
                                    <td>{category.id}</td>
                                    <td>{category.name}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    )
}
