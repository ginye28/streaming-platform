import { useState } from 'react'
import {
    createStream,
    getCategories,
    getStream,
    updateStream,
    uploadFile,
} from '../api.js'
import { assetUrl } from '../assets.js'
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

    /** @returns 업로드 결과. 실패하면 null. */
    async function handleUpload(event) {
        const file = event.target.files?.[0]

        if (!file) return null

        setBusy(true)
        setError(null)

        try {
            return await uploadFile(file)
        } catch (e) {
            setError(e.message)
            return null
        } finally {
            setBusy(false)
        }
    }

    async function handleVideoUpload(event) {
        const uploaded = await handleUpload(event)

        if (!uploaded) return

        setVideoUrl(uploaded.url)

        // 서버가 첫 장면을 뽑아 줬으면 썸네일로 쓴다.
        // 직접 고른 썸네일이 이미 있으면 그대로 둔다.
        if (uploaded.thumbnailUrl) {
            setThumbnailUrl((current) => current || uploaded.thumbnailUrl)
        }
    }

    async function handleThumbnailUpload(event) {
        const uploaded = await handleUpload(event)

        if (uploaded) {
            setThumbnailUrl(uploaded.url)
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
                        onChange={handleVideoUpload}
                    />
                </label>
                {videoUrl && <p className="meta">업로드됨: {videoUrl}</p>}

                <label>
                    썸네일
                    <input
                        type="file"
                        accept="image/*"
                        onChange={handleThumbnailUpload}
                    />
                </label>
                {thumbnailUrl && (
                    <div className="thumbnail-preview">
                        <img src={assetUrl(thumbnailUrl)} alt="썸네일 미리보기" />
                        <button type="button" onClick={() => setThumbnailUrl('')}>
                            지우기
                        </button>
                    </div>
                )}

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
