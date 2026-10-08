import { useEffect, useRef, useState } from 'react'
import {
    createStream,
    getCategories,
    getStream,
    updateStream,
    uploadFile,
} from '../api.js'
import { assetUrl } from '../assets.js'
import { setFlash } from '../flash.js'
import { navigate } from '../router.js'
import { useAsyncData } from '../useAsyncData.js'
import { usePageTitle } from '../usePageTitle.js'
import { useSlow } from '../useSlow.js'

// 서버(FileUploadService, multipart 설정)가 받는 형식과 크기. 서버에서 거절당하기 전에 알려 준다.
const VIDEO_EXTENSIONS = ['mp4', 'mov', 'webm', 'm4v']
const IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'webp', 'gif']
const MAX_BYTES = 100 * 1024 * 1024
const TITLE_MAX = 100

function extensionOf(name) {
    const dot = name.lastIndexOf('.')

    return dot < 0 ? '' : name.slice(dot + 1).toLowerCase()
}

function formatSize(bytes) {
    if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)}MB`

    return `${Math.max(1, Math.round(bytes / 1024))}KB`
}

/** 올리기 전에 형식과 크기를 본다. 문제가 있으면 사용자에게 보일 문장을, 없으면 null. */
function checkFile(file, kind) {
    const allowed = kind === 'video' ? VIDEO_EXTENSIONS : IMAGE_EXTENSIONS
    const what = kind === 'video' ? '영상' : '이미지'

    if (!allowed.includes(extensionOf(file.name))) {
        return `${allowed.join(', ')} 형식의 ${what}만 올릴 수 있어요. (고른 파일: ${file.name})`
    }

    if (file.size > MAX_BYTES) {
        return `${formatSize(MAX_BYTES).replace('.0', '')} 이하만 올릴 수 있어요. (고른 파일: ${formatSize(file.size)})`
    }

    return null
}

/** 파일 이름에서 확장자를 떼어 제목 후보로 쓴다. */
function titleFromName(name) {
    return name.replace(/\.[^.]+$/, '').replace(/[_]+/g, ' ').trim().slice(0, TITLE_MAX)
}

/**
 * 파일 하나를 올리는 상태. idle → uploading → (성공이면 idle 로 돌아가고 onDone, 실패면 error).
 * 진행률과 취소를 갖고, 화면을 떠나면 진행 중인 업로드를 멈춘다.
 */
function useUploader(kind, onDone) {
    const [state, setState] = useState({ phase: 'idle' })
    const controllerRef = useRef(null)

    useEffect(() => () => controllerRef.current?.abort(), [])

    async function start(file) {
        const problem = checkFile(file, kind)

        if (problem) {
            setState({ phase: 'error', error: problem })
            return
        }

        controllerRef.current?.abort()

        const controller = new AbortController()
        controllerRef.current = controller

        setState({ phase: 'uploading', name: file.name, size: file.size, progress: 0 })

        try {
            const uploaded = await uploadFile(file, {
                signal: controller.signal,
                onProgress: (progress) =>
                    setState((current) =>
                        current.phase === 'uploading' ? { ...current, progress } : current
                    ),
            })

            if (controller.signal.aborted) return

            setState({ phase: 'idle' })
            onDone(uploaded, file)
        } catch (e) {
            if (controller.signal.aborted) return

            setState({ phase: 'error', error: e.message })
        }
    }

    function cancel() {
        controllerRef.current?.abort()
        setState({ phase: 'idle' })
    }

    function clearError() {
        setState((current) => (current.phase === 'error' ? { phase: 'idle' } : current))
    }

    return { ...state, start, cancel, clearError }
}

/** id 가 있으면 수정, 없으면 새 영상 등록. */
export default function UploadPage({ id }) {
    const { data: categories } = useAsyncData(getCategories, [])

    const {
        data: existing,
        error: loadError,
        loading,
        reload,
    } = useAsyncData(() => (id ? getStream(id) : Promise.resolve(null)), [id])

    usePageTitle(id ? '영상 수정' : '영상 올리기')

    // 수정 화면은 기존 값을 받은 뒤에 폼을 만든다.
    // 이렇게 해야 이펙트로 폼 상태를 되맞추지 않아도 된다.
    if (id && loading) {
        return (
            <p className="empty" role="status">
                불러오는 중…
            </p>
        )
    }

    // 값을 못 받았는데 빈 폼을 "수정"이라고 보여 주면 저장했을 때 영상이 지워진다.
    if (id && !existing) {
        return (
            <section className="narrow">
                <h1>영상 수정</h1>
                <p className="error" role="alert">
                    {loadError ?? '영상을 찾을 수 없어요.'}
                </p>
                <div className="upload__actions">
                    <button type="button" onClick={reload}>
                        다시 시도
                    </button>
                    <button type="button" onClick={() => navigate({ view: 'home' })}>
                        홈으로
                    </button>
                </div>
            </section>
        )
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
    const initial = {
        title: existing?.title ?? '',
        description: existing?.description ?? '',
        videoUrl: existing?.videoUrl ?? '',
        thumbnailUrl: existing?.thumbnailUrl ?? '',
        categoryId: existing?.categoryId ?? '',
    }

    const [title, setTitle] = useState(initial.title)
    const [description, setDescription] = useState(initial.description)
    const [videoUrl, setVideoUrl] = useState(initial.videoUrl)
    // 방금 올린 영상의 이름과 크기. 수정 화면에서 기존 영상을 그대로 둘 때는 없다.
    const [videoFile, setVideoFile] = useState(null)
    // auto: 서버가 영상의 첫 장면으로 만들어 준 썸네일인지. 직접 올리면 아니다.
    const [thumb, setThumb] = useState({ url: initial.thumbnailUrl, auto: false })
    const [brokenThumbnail, setBrokenThumbnail] = useState(null)
    const [categoryId, setCategoryId] = useState(initial.categoryId)
    const [error, setError] = useState(null)
    const [busy, setBusy] = useState(false)
    const [dragging, setDragging] = useState(false)

    const videoInputRef = useRef(null)
    const thumbnailInputRef = useRef(null)
    const savedRef = useRef(false)

    const video = useUploader('video', (uploaded, file) => {
        setVideoUrl(uploaded.url)
        setVideoFile({ name: file.name, size: file.size })
        // 제목을 아직 안 썼으면 파일 이름으로 채워 준다. 고쳐 쓰면 된다.
        setTitle((current) => current || titleFromName(file.name))

        // 서버가 첫 장면을 뽑아 줬으면 썸네일로 쓴다. 직접 고른 썸네일이 이미 있으면 그대로 둔다.
        if (uploaded.thumbnailUrl) {
            setThumb((current) =>
                current.url ? current : { url: uploaded.thumbnailUrl, auto: true }
            )
        }
    })

    const thumbnail = useUploader('image', (uploaded) => {
        setThumb({ url: uploaded.url, auto: false })
    })

    const uploading = video.phase === 'uploading' || thumbnail.phase === 'uploading'

    usePageTitle(id ? '영상 수정' : '영상 올리기')

    const dirty =
        title !== initial.title ||
        description !== initial.description ||
        videoUrl !== initial.videoUrl ||
        thumb.url !== initial.thumbnailUrl ||
        categoryId !== initial.categoryId

    // 올리는 중이거나 쓴 내용이 있는데 탭을 닫으면 브라우저가 한 번 물어본다.
    useEffect(() => {
        if (!uploading && !dirty) return

        function warn(event) {
            if (savedRef.current) return

            event.preventDefault()
        }

        window.addEventListener('beforeunload', warn)

        return () => window.removeEventListener('beforeunload', warn)
    }, [uploading, dirty])

    function pick(uploader, event) {
        const file = event.target.files?.[0]

        // 같은 파일을 다시 골라도 change 가 오도록 비워 둔다.
        event.target.value = ''

        if (file) uploader.start(file)
    }

    function handleDrop(event) {
        event.preventDefault()
        setDragging(false)

        const file = event.dataTransfer.files?.[0]

        if (file) video.start(file)
    }

    async function handleSubmit(event) {
        event.preventDefault()

        setBusy(true)
        setError(null)

        const payload = {
            title: title.trim(),
            description: description || null,
            videoUrl,
            thumbnailUrl: thumb.url || null,
            categoryId: categoryId ? Number(categoryId) : null,
        }

        try {
            const saved = id ? await updateStream(id, payload) : await createStream(payload)

            savedRef.current = true
            setFlash(
                id
                    ? '저장했어요.'
                    : '올렸어요. 주소창의 링크를 복사해 공유할 수 있어요.'
            )
            navigate({ view: 'stream', id: saved.id })
        } catch (e) {
            setError(e.message)
            setBusy(false)
        }
    }

    const canSubmit = Boolean(videoUrl) && !uploading && !busy
    const submitHint = !videoUrl
        ? '영상을 올리면 등록할 수 있어요.'
        : uploading
          ? '올리는 중이에요. 끝나면 저장할 수 있어요.'
          : null

    return (
        <section className="narrow upload">
            <h1>{id ? '영상 수정' : '영상 올리기'}</h1>
            <p className="upload__lede">
                {id
                    ? '제목과 설명, 썸네일을 바꿀 수 있어요. 영상은 바꾸지 않으면 지금 것 그대로예요.'
                    : '영상을 먼저 고르세요. 제목은 파일 이름으로 채워 드려요.'}
            </p>

            <form className="form" onSubmit={handleSubmit}>
                <div className="upload__slot" role="group" aria-labelledby="upload-video">
                    <p className="upload__label" id="upload-video">
                        영상 <span className="upload__tag">필수</span>
                    </p>

                    <input
                        ref={videoInputRef}
                        type="file"
                        hidden
                        accept={VIDEO_EXTENSIONS.map((ext) => `.${ext}`).join(',')}
                        onChange={(event) => pick(video, event)}
                    />

                    {video.phase === 'uploading' ? (
                        <UploadProgress
                            name={video.name}
                            size={video.size}
                            progress={video.progress}
                            onCancel={video.cancel}
                            wake
                        />
                    ) : videoUrl ? (
                        <div className="upload__done">
                            <p role="status">
                                {videoFile
                                    ? `✓ 올라갔어요 · ${videoFile.name} (${formatSize(videoFile.size)})`
                                    : '✓ 지금 올라가 있는 영상을 쓰고 있어요.'}
                            </p>
                            <button
                                type="button"
                                onClick={() => videoInputRef.current?.click()}
                            >
                                다른 영상으로 바꾸기
                            </button>
                        </div>
                    ) : (
                        <button
                            type="button"
                            className={`upload__drop${dragging ? ' upload__drop--over' : ''}`}
                            onClick={() => videoInputRef.current?.click()}
                            onDragOver={(event) => {
                                event.preventDefault()
                                setDragging(true)
                            }}
                            onDragLeave={() => setDragging(false)}
                            onDrop={handleDrop}
                        >
                            <strong>영상을 끌어다 놓거나, 눌러서 고르기</strong>
                            <span>
                                {VIDEO_EXTENSIONS.join(' · ')} · {formatSize(MAX_BYTES).replace('.0', '')} 이하
                            </span>
                        </button>
                    )}

                    {video.phase === 'error' && (
                        <p className="error" role="alert">
                            {video.error}
                        </p>
                    )}
                </div>

                <label>
                    <span>
                        제목 <span className="upload__tag">필수</span>
                    </span>
                    <input
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        maxLength={TITLE_MAX}
                        required
                    />
                </label>

                <div className="upload__slot" role="group" aria-labelledby="upload-thumbnail">
                    <p className="upload__label" id="upload-thumbnail">
                        썸네일 <span className="upload__tag upload__tag--soft">선택</span>
                    </p>

                    <input
                        ref={thumbnailInputRef}
                        type="file"
                        hidden
                        accept={IMAGE_EXTENSIONS.map((ext) => `.${ext}`).join(',')}
                        onChange={(event) => pick(thumbnail, event)}
                    />

                    {thumbnail.phase === 'uploading' ? (
                        <UploadProgress
                            name={thumbnail.name}
                            size={thumbnail.size}
                            progress={thumbnail.progress}
                            onCancel={thumbnail.cancel}
                        />
                    ) : thumb.url ? (
                        <div className="thumbnail-preview">
                            {brokenThumbnail === thumb.url ? (
                                <p className="upload__broken">미리보기를 불러오지 못했어요.</p>
                            ) : (
                                <img
                                    src={assetUrl(thumb.url)}
                                    alt="썸네일 미리보기"
                                    onError={() => setBrokenThumbnail(thumb.url)}
                                />
                            )}
                            <div className="upload__thumb-side">
                                {thumb.auto && (
                                    <p className="meta">영상 첫 장면으로 자동으로 만들었어요.</p>
                                )}
                                <button
                                    type="button"
                                    onClick={() => thumbnailInputRef.current?.click()}
                                >
                                    다른 이미지 올리기
                                </button>
                                <button
                                    type="button"
                                    aria-label="썸네일 지우기"
                                    onClick={() => setThumb({ url: '', auto: false })}
                                >
                                    지우기
                                </button>
                            </div>
                        </div>
                    ) : (
                        <div className="upload__thumb-empty">
                            <button
                                type="button"
                                onClick={() => thumbnailInputRef.current?.click()}
                            >
                                이미지 고르기
                            </button>
                            <p className="meta">
                                영상을 올리면 첫 장면으로 만들어요(만들지 못하면 비어 있어요).{' '}
                                {IMAGE_EXTENSIONS.join(' · ')}, 100MB 이하.
                            </p>
                        </div>
                    )}

                    {thumbnail.phase === 'error' && (
                        <p className="error" role="alert">
                            {thumbnail.error}
                        </p>
                    )}
                </div>

                <label>
                    <span>
                        설명 <span className="upload__tag upload__tag--soft">선택</span>
                    </span>
                    <textarea
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        rows={4}
                    />
                </label>

                <label>
                    <span>
                        카테고리 <span className="upload__tag upload__tag--soft">선택</span>
                    </span>
                    <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
                        <option value="">선택 안 함</option>
                        {categories?.map((category) => (
                            <option key={category.id} value={category.id}>
                                {category.name}
                            </option>
                        ))}
                    </select>
                </label>

                {error && (
                    <p className="error" role="alert">
                        {error}
                    </p>
                )}

                <div className="upload__actions">
                    <button className="button--primary" type="submit" disabled={!canSubmit}>
                        {busy ? '저장하는 중…' : id ? '저장' : '등록'}
                    </button>
                    {id && (
                        <button type="button" onClick={() => navigate({ view: 'stream', id })}>
                            취소
                        </button>
                    )}
                    {submitHint && <p className="meta upload__hint">{submitHint}</p>}
                </div>
            </form>
        </section>
    )
}

/** 올리는 중 카드. 파일 이름·크기, 진행률, 취소. 서버가 잠들어 시작을 못 할 때는 그 이유도 알린다. */
function UploadProgress({ name, size, progress, onCancel, wake = false }) {
    // 보낸 바이트가 아직 0 인 채 오래 가면 서버가 깨어나는 중이다.
    const slow = useSlow(wake && progress === 0)
    const percent = Math.round(progress * 100)

    const message = slow
        ? '서버를 깨우는 중이에요. 최대 1분쯤 걸려요. 이 화면을 닫지 마세요.'
        : progress >= 1
          ? '서버에서 저장하는 중이에요…'
          : '올리는 중이에요. 이 화면을 닫지 마세요.'

    return (
        <div className="upload__progress">
            <p className="upload__file">
                <strong>{name}</strong> · {formatSize(size)}
            </p>
            <div className="upload__bar">
                <progress value={progress} max={1} aria-label={`${name} 올리는 중`} />
                <span aria-hidden="true">{percent}%</span>
            </div>
            <p className="meta" role="status">
                {message}
            </p>
            <button type="button" onClick={onCancel}>
                취소
            </button>
        </div>
    )
}
