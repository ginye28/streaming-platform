import { useState } from 'react'
import {
    changePassword,
    getLiveSetting,
    getMyBlocks,
    getMySubscriptions,
    getStreamKey,
    regenerateStreamKey,
    toggleBlock,
    updateLiveSetting,
    updateProfile,
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
