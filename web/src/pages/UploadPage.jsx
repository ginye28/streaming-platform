import { useState } from 'react'
import {
    createStream,
    getCategories,
    getStream,
    updateStream,
    uploadFile,
} from '../api.js'
import { navigate } from '../router.js'
import { useAsyncData } from '../useAsyncData.js'

/** id 가 있으면 수정, 없으면 새 영상 등록. */
export default function UploadPage({ id }) {
    const { data: categories } = useAsyncData(getCategories, [])

    const { data: existing, loading } = useAsyncData(
        () => (id ? getStream(id) : Promise.resolve(null)),
        [id]
    )

    // 수정 화면은 기존 값을 받은 뒤에 폼을 만든다.
    // 이렇게 해야 이펙트로 폼 상태를 되맞추지 않아도 된다.
    if (id && loading) {
        return <p className="empty">불러오는 중…</p>
    }

    return (
        <StreamForm
            key={existing?.id ?? 'new'}
            id={id}
            existing={existing}
            categories={categories}
        />
    )
}

function StreamForm({ id, existing, categories }) {
    const [title, setTitle] = useState(existing?.title ?? '')
    const [description, setDescription] = useState(existing?.description ?? '')
    const [videoUrl, setVideoUrl] = useState(existing?.videoUrl ?? '')
    const [thumbnailUrl, setThumbnailUrl] = useState(existing?.thumbnailUrl ?? '')
    const [categoryId, setCategoryId] = useState(existing?.categoryId ?? '')
    const [error, setError] = useState(null)
    const [busy, setBusy] = useState(false)

    async function handleUpload(event, setter) {
        const file = event.target.files?.[0]

        if (!file) return

        setBusy(true)
        setError(null)

        try {
            const { url } = await uploadFile(file)
            setter(url)
        } catch (e) {
            setError(e.message)
        } finally {
            setBusy(false)
        }
    }

    async function handleSubmit(event) {
        event.preventDefault()

        setBusy(true)
        setError(null)

        const payload = {
            title,
            description: description || null,
            videoUrl,
            thumbnailUrl: thumbnailUrl || null,
            categoryId: categoryId ? Number(categoryId) : null,
        }

        try {
            const saved = id ? await updateStream(id, payload) : await createStream(payload)
            navigate({ view: 'stream', id: saved.id })
        } catch (e) {
            setError(e.message)
        } finally {
            setBusy(false)
        }
    }

    return (
        <section className="narrow">
            <h2>{id ? '영상 수정' : '영상 올리기'}</h2>

            {error && <p className="error">{error}</p>}

            <form className="form" onSubmit={handleSubmit}>
                <label>
                    제목
                    <input value={title} onChange={(e) => setTitle(e.target.value)} required />
                </label>

                <label>
                    설명
                    <textarea
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        rows={4}
                    />
                </label>

                <label>
                    영상 파일
                    <input
                        type="file"
                        accept="video/*"
                        onChange={(e) => handleUpload(e, setVideoUrl)}
                    />
                </label>
                {videoUrl && <p className="meta">업로드됨: {videoUrl}</p>}

                <label>
                    썸네일
                    <input
                        type="file"
                        accept="image/*"
                        onChange={(e) => handleUpload(e, setThumbnailUrl)}
                    />
                </label>
                {thumbnailUrl && <p className="meta">업로드됨: {thumbnailUrl}</p>}

                <label>
                    카테고리
                    <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
                        <option value="">선택 안 함</option>
                        {categories?.map((category) => (
                            <option key={category.id} value={category.id}>
                                {category.name}
                            </option>
                        ))}
                    </select>
                </label>

                <button type="submit" disabled={busy || !videoUrl}>
                    {busy ? '처리 중…' : id ? '수정' : '등록'}
                </button>
            </form>
        </section>
    )
}
