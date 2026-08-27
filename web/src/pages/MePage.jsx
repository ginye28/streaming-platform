import { useState } from 'react'
import {
    changePassword,
    getLiveSetting,
    getMyBlocks,
    getMyIntro,
    getMySubscriptions,
    getStreamKey,
    regenerateStreamKey,
    toggleBlock,
    updateLiveSetting,
    updateMyIntro,
    updateProfile,
    uploadFile,
} from '../api.js'
import { useAuth } from '../useAuth.js'
import Link from '../components/Link.jsx'
import { useAsyncData } from '../useAsyncData.js'

export default function MePage() {
    const { me, refreshMe } = useAuth()

    if (!me) {
        return <p className="empty">로그인이 필요합니다.</p>
    }

    return (
        <section className="narrow">
            <h2>내 계정</h2>

            <ProfileForm me={me} onSaved={refreshMe} />
            <PasswordForm />
            <StreamKeyPanel />
            <LiveSettingForm />
            <IntroForm />
            <SubscriptionList />
            <BlockList />
        </section>
    )
}

function ProfileForm({ me, onSaved }) {
    const [nickname, setNickname] = useState(me.nickname)
    const [profileImage, setProfileImage] = useState(me.profileImage ?? '')
    const [message, setMessage] = useState(null)
    const [error, setError] = useState(null)

    async function handleSubmit(event) {
        event.preventDefault()
        setError(null)
        setMessage(null)

        try {
            await updateProfile(nickname, profileImage || null)
            await onSaved()
            setMessage('저장했습니다.')
        } catch (e) {
            setError(e.message)
        }
    }

    return (
        <details open>
            <summary>프로필</summary>

            {error && <p className="error">{error}</p>}
            {message && <p className="meta">{message}</p>}

            <form className="form" onSubmit={handleSubmit}>
                <label>
                    닉네임
                    <input
                        value={nickname}
                        onChange={(e) => setNickname(e.target.value)}
                        minLength={2}
                        maxLength={20}
                        required
                    />
                </label>

                <label>
                    프로필 이미지 경로
                    <input
                        value={profileImage}
                        onChange={(e) => setProfileImage(e.target.value)}
                        placeholder="/uploads/p.png"
                    />
                </label>

                <button type="submit">저장</button>
            </form>
        </details>
    )
}

function PasswordForm() {
    const [currentPassword, setCurrentPassword] = useState('')
    const [newPassword, setNewPassword] = useState('')
    const [message, setMessage] = useState(null)
    const [error, setError] = useState(null)

    async function handleSubmit(event) {
        event.preventDefault()
        setError(null)
        setMessage(null)

        try {
            await changePassword(currentPassword, newPassword)
            setCurrentPassword('')
            setNewPassword('')
            setMessage('비밀번호를 변경했습니다.')
        } catch (e) {
            setError(e.message)
        }
    }

    return (
        <details>
            <summary>비밀번호 변경</summary>

            {error && <p className="error">{error}</p>}
            {message && <p className="meta">{message}</p>}

            <form className="form" onSubmit={handleSubmit}>
                <input
                    type="password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="현재 비밀번호"
                    autoComplete="current-password"
                    required
                />
                <input
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="새 비밀번호 (8자 이상)"
                    autoComplete="new-password"
                    minLength={8}
                    required
                />
                <button type="submit">변경</button>
            </form>
        </details>
    )
}

function StreamKeyPanel() {
    const { data, error, reload, fail } = useAsyncData(getStreamKey, [])
    const [revealed, setRevealed] = useState(false)

    async function handleRegenerate() {
        if (!window.confirm('스트림 키를 새로 발급하면 기존 키로는 송출할 수 없습니다. 계속할까요?'))
            return

        try {
            await regenerateStreamKey()
            reload()
        } catch (e) {
            fail(e)
        }
    }

    return (
        <details>
            <summary>송출 설정 (OBS)</summary>

            {error && <p className="error">{error}</p>}

            <p className="meta">서버: rtmp://localhost:1935/live</p>

            <p className="meta">
                스트림 키:{' '}
                {revealed ? <code>{data?.streamKey}</code> : <code>••••••••••••</code>}
            </p>

            <div className="toolbar">
                <button onClick={() => setRevealed((v) => !v)}>
                    {revealed ? '숨기기' : '보기'}
                </button>
                <button onClick={handleRegenerate}>재발급</button>
            </div>
        </details>
    )
}

function LiveSettingForm() {
    const { data: setting, error, loading, fail } = useAsyncData(getLiveSetting, [])

    return (
        <details>
            <summary>다음 방송 정보</summary>

            {error && <p className="error">{error}</p>}
            {loading && <p className="empty">불러오는 중…</p>}

            {/* 저장된 값을 받은 뒤에 폼을 만든다. */}
            {!loading && setting && <LiveSettingFields setting={setting} onFail={fail} />}
        </details>
    )
}

function LiveSettingFields({ setting, onFail }) {
    const [title, setTitle] = useState(setting.title ?? '')
    const [description, setDescription] = useState(setting.description ?? '')
    const [thumbnailUrl, setThumbnailUrl] = useState(setting.thumbnailUrl ?? '')
    const [message, setMessage] = useState(null)

    async function handleSubmit(event) {
        event.preventDefault()
        setMessage(null)

        try {
            await updateLiveSetting({
                title,
                description: description || null,
                thumbnailUrl: thumbnailUrl || null,
            })
            setMessage('저장했습니다. 다음 방송부터 적용됩니다.')
        } catch (e) {
            onFail(e)
        }
    }

    return (
        <>
            {message && <p className="meta">{message}</p>}

            <form className="form" onSubmit={handleSubmit}>
                <label>
                    방송 제목
                    <input value={title} onChange={(e) => setTitle(e.target.value)} required />
                </label>

                <label>
                    설명
                    <textarea
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        rows={3}
                    />
                </label>

                <label>
                    썸네일 경로
                    <input
                        value={thumbnailUrl}
                        onChange={(e) => setThumbnailUrl(e.target.value)}
                        placeholder="/uploads/t.png"
                    />
                </label>

                <button type="submit">저장</button>
            </form>
        </>
    )
}

/**
 * 처음 들어온 시청자에게 보여 줄 자기소개.
 * 방송마다 바뀌는 제목과 달리 "이 사람이 누구인가" 는 그대로라 따로 둔다.
 */
function IntroForm() {
    const { data: intro, error, loading, fail } = useAsyncData(getMyIntro, [])

    return (
        <details>
            <summary>첫 방문자에게 보여 줄 소개</summary>

            {error && <p className="error">{error}</p>}
            {loading && <p className="empty">불러오는 중…</p>}

            {!loading && intro && <IntroFields intro={intro} onFail={fail} />}
        </details>
    )
}

function IntroFields({ intro, onFail }) {
    const [videoUrl, setVideoUrl] = useState(intro.videoUrl ?? '')
    const [headline, setHeadline] = useState(intro.headline ?? '')
    const [greeting, setGreeting] = useState(intro.greeting ?? '')
    const [message, setMessage] = useState(null)
    const [busy, setBusy] = useState(false)

    async function handleUpload(event) {
        const file = event.target.files?.[0]

        if (!file) return

        setBusy(true)
        setMessage(null)

        try {
            const uploaded = await uploadFile(file)
            setVideoUrl(uploaded.url)
        } catch (e) {
            onFail(e)
        } finally {
            setBusy(false)
        }
    }

    async function handleSubmit(event) {
        event.preventDefault()
        setMessage(null)

        try {
            await updateMyIntro({
                videoUrl: videoUrl || null,
                headline: headline || null,
                greeting: greeting || null,
            })
            setMessage('저장했습니다. 처음 들어온 시청자에게 보입니다.')
        } catch (e) {
            onFail(e)
        }
    }

    return (
        <>
            <p className="meta">
                내 방송에 처음 들어온 사람에게 먼저 보여 줍니다. 이미 구독한 사람이나 한 번 본
                사람에게는 다시 뜨지 않습니다. 비워 두면 바로 방송이 시작됩니다.
            </p>

            {message && <p className="meta">{message}</p>}

            <form className="form" onSubmit={handleSubmit}>
                <label>
                    소개 영상 (선택)
                    <input type="file" accept="video/*" onChange={handleUpload} />
                </label>
                {videoUrl && (
                    <p className="meta">
                        올린 영상: {videoUrl}{' '}
                        <button type="button" onClick={() => setVideoUrl('')}>
                            지우기
                        </button>
                    </p>
                )}

                <label>
                    한 줄 소개
                    <input
                        value={headline}
                        onChange={(e) => setHeadline(e.target.value)}
                        maxLength={60}
                        placeholder="주로 게임 방송을 합니다"
                    />
                </label>

                <label>
                    소개글
                    <textarea
                        value={greeting}
                        onChange={(e) => setGreeting(e.target.value)}
                        rows={4}
                        placeholder="처음 오신 분들께 하고 싶은 말"
                    />
                </label>

                <button type="submit" disabled={busy}>
                    {busy ? '올리는 중…' : '저장'}
                </button>
            </form>
        </>
    )
}

function SubscriptionList() {
    const { data: page, error } = useAsyncData(() => getMySubscriptions(0), [])

    return (
        <details>
            <summary>구독 중인 채널</summary>

            {error && <p className="error">{error}</p>}
            {page?.content.length === 0 && <p className="empty">구독 중인 채널이 없습니다.</p>}

            <ul>
                {page?.content.map((channel) => (
                    <li key={channel.id}>
                        <Link to={{ view: 'channel', id: channel.id }}>{channel.nickname}</Link>
                        {channel.live && <span className="meta"> · 방송 중</span>}
                    </li>
                ))}
            </ul>
        </details>
    )
}

function BlockList() {
    const { data: page, error, reload, fail } = useAsyncData(() => getMyBlocks(0), [])

    async function handleUnblock(userId) {
        try {
            await toggleBlock(userId)
            reload()
        } catch (e) {
            fail(e)
        }
    }

    return (
        <details>
            <summary>차단한 사용자</summary>

            {error && <p className="error">{error}</p>}
            {page?.content.length === 0 && <p className="empty">차단한 사용자가 없습니다.</p>}

            <ul>
                {page?.content.map((user) => (
                    <li key={user.id}>
                        {user.nickname}{' '}
                        <button onClick={() => handleUnblock(user.id)}>차단 해제</button>
                    </li>
                ))}
            </ul>
        </details>
    )
}
