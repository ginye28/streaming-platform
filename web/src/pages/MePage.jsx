import { useState } from 'react'
import {
    changePassword,
    getLiveSetting,
    getMyBlocks,
    cancelPayment,
    getMyChannelProfile,
    getMyEarnings,
    getMyIntro,
    getMyPayments,
    getMySubscriptions,
    getStreamKey,
    regenerateStreamKey,
    toggleBlock,
    updateLiveSetting,
    updateMyChannelProfile,
    updateMyIntro,
    updateProfile,
    uploadFile,
} from '../api.js'
import { assetUrl } from '../assets.js'
import { useAuth } from '../useAuth.js'
import { AUDIENCE_OPTIONS, SLOW_MODE_CHOICES } from '../audience.js'
import { CREDIT_ROLES } from '../components/ChannelIdentity.jsx'
import ChatTools from '../components/ChatTools.jsx'
import Link from '../components/Link.jsx'
import ScheduleManager from '../components/ScheduleManager.jsx'
import SubscriptionControls from '../components/SubscriptionControls.jsx'
import { dateOnly, won } from '../payments.js'
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
            <ScheduleManager />
            <ChatTools />
            <IntroForm />
            <ChannelProfileForm />
            <SubscriptionList />
            <PaymentHistory />
            <EarningsPanel />
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
    const [audience, setAudience] = useState(setting.audience ?? 'ALL')
    const [chatAudience, setChatAudience] = useState(setting.chatAudience ?? 'ALL')
    const [slowModeSeconds, setSlowModeSeconds] = useState(setting.slowModeSeconds ?? 0)
    const [message, setMessage] = useState(null)

    async function handleSubmit(event) {
        event.preventDefault()
        setMessage(null)

        try {
            await updateLiveSetting({
                title,
                description: description || null,
                thumbnailUrl: thumbnailUrl || null,
                audience,
                chatAudience,
                slowModeSeconds,
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

                <label>
                    방송을 볼 수 있는 사람
                    <select value={audience} onChange={(e) => setAudience(e.target.value)}>
                        {AUDIENCE_OPTIONS.map(({ value, label }) => (
                            <option key={value} value={value}>
                                {label}
                            </option>
                        ))}
                    </select>
                    <span className="meta">
                        구독자만 보게 하면 구독하지 않은 사람에게는 영상 주소가 내려가지 않습니다. 방송 중에는 바꿀 수 없습니다.
                    </span>
                </label>

                <label>
                    채팅할 수 있는 사람
                    <select value={chatAudience} onChange={(e) => setChatAudience(e.target.value)}>
                        {AUDIENCE_OPTIONS.map(({ value, label }) => (
                            <option key={value} value={value}>
                                {label}
                            </option>
                        ))}
                    </select>
                </label>

                <label>
                    슬로우 모드
                    <select value={slowModeSeconds} onChange={(e) => setSlowModeSeconds(Number(e.target.value))}>
                        {SLOW_MODE_CHOICES.map((seconds) => (
                            <option key={seconds} value={seconds}>
                                {seconds === 0 ? '끔' : `${seconds}초에 한 번`}
                            </option>
                        ))}
                    </select>
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

/** 오시마크 · 팬네임 · 데뷔/졸업 · 모델 크레딧. */
function ChannelProfileForm() {
    const { data: profile, error, loading, fail } = useAsyncData(getMyChannelProfile, [])

    return (
        <details>
            <summary>채널 정보</summary>

            {error && <p className="error">{error}</p>}
            {loading && <p className="empty">불러오는 중…</p>}

            {!loading && profile && <ChannelProfileFields profile={profile} onFail={fail} />}
        </details>
    )
}

function ChannelProfileFields({ profile, onFail }) {
    const [oshiMarkUrl, setOshiMarkUrl] = useState(profile.oshiMarkUrl ?? '')
    const [paidOshiMarkUrl, setPaidOshiMarkUrl] = useState(profile.paidOshiMarkUrl ?? '')
    const [fanName, setFanName] = useState(profile.fanName ?? '')
    const [debutOn, setDebutOn] = useState(profile.debutOn ?? '')
    const [graduatedOn, setGraduatedOn] = useState(profile.graduatedOn ?? '')
    const [credits, setCredits] = useState(profile.credits)
    const [message, setMessage] = useState(null)
    const [busy, setBusy] = useState(false)

    /** 고른 이미지를 올리고, 돌려받은 주소를 setUrl 로 넘기는 핸들러를 만든다. */
    function markUploader(setUrl) {
        return async (event) => {
            const file = event.target.files?.[0]

            if (!file) return

            setBusy(true)
            setMessage(null)

            try {
                const uploaded = await uploadFile(file)
                setUrl(uploaded.url)
            } catch (e) {
                onFail(e)
            } finally {
                setBusy(false)
            }
        }
    }

    function updateCredit(index, patch) {
        setCredits(credits.map((credit, i) => (i === index ? { ...credit, ...patch } : credit)))
    }

    async function handleSubmit(event) {
        event.preventDefault()
        setMessage(null)

        try {
            await updateMyChannelProfile({
                oshiMarkUrl: oshiMarkUrl || null,
                paidOshiMarkUrl: paidOshiMarkUrl || null,
                fanName: fanName || null,
                debutOn: debutOn || null,
                graduatedOn: graduatedOn || null,
                // 이름이 빈 줄은 보내지 않는다. 서버가 400 으로 막는 값이다.
                credits: credits.filter((credit) => credit.name.trim()),
            })
            setMessage('저장했습니다.')
        } catch (e) {
            onFail(e)
        }
    }

    return (
        <>
            {message && <p className="meta">{message}</p>}

            <form className="form" onSubmit={handleSubmit}>
                <MarkField
                    label="오시마크"
                    hint="구독한 사람의 이름 옆(채팅·댓글)에 이 표식이 붙습니다. 작게 보이니 단순한 그림이 좋습니다."
                    value={oshiMarkUrl}
                    onUpload={markUploader(setOshiMarkUrl)}
                    onClear={() => setOshiMarkUrl('')}
                />

                <MarkField
                    label="유료 구독자용 오시마크"
                    hint="유료로 구독한 사람에게는 위 표식 대신 이 표식이 붙습니다. 비워 두면 유료 구독자도 위 표식을 답니다."
                    value={paidOshiMarkUrl}
                    onUpload={markUploader(setPaidOshiMarkUrl)}
                    onClear={() => setPaidOshiMarkUrl('')}
                />

                <label>
                    팬네임
                    <input
                        value={fanName}
                        onChange={(e) => setFanName(e.target.value)}
                        maxLength={30}
                        placeholder="별무리"
                    />
                </label>
                <p className="meta">채널에서 &quot;구독자 N명&quot; 대신 이 이름으로 보입니다.</p>

                <label>
                    데뷔일
                    <input
                        type="date"
                        value={debutOn}
                        onChange={(e) => setDebutOn(e.target.value)}
                    />
                </label>

                <label>
                    졸업일
                    <input
                        type="date"
                        value={graduatedOn}
                        onChange={(e) => setGraduatedOn(e.target.value)}
                    />
                </label>
                <p className="meta">
                    졸업일을 넣으면 채널에 졸업으로 표시됩니다. 올린 영상과 지난 방송은 그대로
                    남습니다.
                </p>

                <fieldset className="credits">
                    <legend>만들어 주신 분들</legend>

                    {credits.map((credit, index) => (
                        <div className="credits__row" key={index}>
                            <select
                                value={credit.role}
                                onChange={(e) => updateCredit(index, { role: e.target.value })}
                            >
                                {Object.entries(CREDIT_ROLES).map(([value, label]) => (
                                    <option key={value} value={value}>
                                        {label}
                                    </option>
                                ))}
                            </select>

                            <input
                                value={credit.name}
                                onChange={(e) => updateCredit(index, { name: e.target.value })}
                                placeholder="이름"
                                maxLength={60}
                            />

                            <input
                                value={credit.link ?? ''}
                                onChange={(e) => updateCredit(index, { link: e.target.value })}
                                placeholder="주소 (선택)"
                            />

                            <button
                                type="button"
                                onClick={() => setCredits(credits.filter((_, i) => i !== index))}
                            >
                                삭제
                            </button>
                        </div>
                    ))}

                    <button
                        type="button"
                        onClick={() =>
                            setCredits([...credits, { role: 'ILLUSTRATOR', name: '', link: '' }])
                        }
                    >
                        한 줄 추가
                    </button>
                </fieldset>

                <button type="submit" disabled={busy}>
                    {busy ? '올리는 중…' : '저장'}
                </button>
            </form>
        </>
    )
}

/** 오시마크 이미지 한 장을 고르는 칸. 일반용과 유료용이 같은 모양이다. */
function MarkField({ label, hint, value, onUpload, onClear }) {
    return (
        <>
            <label>
                {label}
                <input type="file" accept="image/*" onChange={onUpload} />
            </label>
            <p className="meta">{hint}</p>
            {value && (
                <p className="meta">
                    <img className="identity__mark" src={assetUrl(value)} alt="" /> {value}{' '}
                    <button type="button" onClick={onClear}>
                        지우기
                    </button>
                </p>
            )}
        </>
    )
}

function SubscriptionList() {
    const { data: page, error, reload, fail } = useAsyncData(() => getMySubscriptions(0), [])

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
                        <SubscriptionControls
                            channelId={channel.id}
                            tier={channel.myTier}
                            paidUntil={channel.myPaidUntil}
                            markVisible={channel.myMarkVisible}
                            onChanged={reload}
                            onFail={fail}
                        />
                    </li>
                ))}
            </ul>
        </details>
    )
}

/**
 * 유료 구독 결제 내역. 승인된 결제와 환불된 결제가 보이고, 영수증은 토스가 열어 준다.
 * 승인 뒤 환불 가능 기간(서버가 정한다) 안이면 직접 환불할 수 있다.
 */
function PaymentHistory() {
    const { data: page, error, reload, fail } = useAsyncData(() => getMyPayments(0), [])

    async function handleRefund(payment) {
        const message =
            `${payment.channelNickname} 유료 구독 ${won(payment.amount)} 을 환불할까요? ` +
            '이 결제로 늘어난 유료 기간이 줄어듭니다.'

        if (!window.confirm(message)) return

        try {
            await cancelPayment(payment.id, '고객 요청 환불')
            reload()
        } catch (e) {
            fail(e)
        }
    }

    return (
        <details>
            <summary>결제 내역</summary>

            {error && <p className="error">{error}</p>}
            {page?.content.length === 0 && <p className="empty">결제 내역이 없습니다.</p>}

            <ul>
                {page?.content.map((payment) => (
                    <li key={payment.id}>
                        <Link to={{ view: 'channel', id: payment.channelId }}>
                            {payment.channelNickname}
                        </Link>
                        <span className="meta">
                            {' '}
                            · {payment.kind === 'DONATION' ? '후원' : '유료 구독'} · {won(payment.amount)} ·{' '}
                            {payment.method} · {dateOnly(payment.approvedAt)}
                        </span>
                        {payment.status === 'CANCELED' && (
                            <span className="meta"> · 환불됨 {dateOnly(payment.canceledAt)}</span>
                        )}
                        {payment.receiptUrl && (
                            <>
                                {' '}
                                <a href={payment.receiptUrl} target="_blank" rel="noreferrer">
                                    영수증
                                </a>
                            </>
                        )}
                        {payment.refundable && (
                            <>
                                {' '}
                                <button type="button" onClick={() => handleRefund(payment)}>
                                    환불
                                </button>
                                <span className="meta">
                                    {' '}
                                    {dateOnly(payment.refundDeadline)}까지
                                </span>
                            </>
                        )}
                    </li>
                ))}
            </ul>
        </details>
    )
}

/**
 * 채널 주인의 수익 장부. 달별 결제·환불·수수료·정산 예정액.
 * 장부일 뿐이라 송금은 하지 않는다. 결제한 사람은 보이지 않는다.
 */
function EarningsPanel() {
    const { data: earnings, error } = useAsyncData(getMyEarnings, [])

    return (
        <details>
            <summary>수익 현황</summary>

            {error && <p className="error">{error}</p>}
            {earnings?.months.length === 0 && (
                <p className="empty">아직 받은 유료 구독이나 후원이 없습니다.</p>
            )}

            {earnings?.months.length > 0 && (
                <>
                    <p>
                        정산 예정액 <strong>{won(earnings.total.net)}</strong>
                        <span className="meta">
                            {' '}
                            · 결제 {won(earnings.total.gross)} − 환불 {won(earnings.total.refunded)} −
                            수수료 {won(earnings.total.fee)} ({earnings.feePercent}%)
                        </span>
                        <br />
                        <span className="meta">
                            유료 구독 {won(earnings.total.membership)} · 후원 {won(earnings.total.donation)} (환불 제외)
                        </span>
                    </p>

                    <table className="earnings">
                        <thead>
                            <tr>
                                <th>달</th>
                                <th>결제</th>
                                <th>구독</th>
                                <th>후원</th>
                                <th>환불</th>
                                <th>수수료</th>
                                <th>정산 예정</th>
                            </tr>
                        </thead>
                        <tbody>
                            {earnings.months.map((month) => (
                                <tr key={month.month}>
                                    <td>{month.month}</td>
                                    <td>
                                        {won(month.gross)} ({month.count}건)
                                    </td>
                                    <td>{won(month.membership)}</td>
                                    <td>{won(month.donation)}</td>
                                    <td>{won(month.refunded)}</td>
                                    <td>{won(month.fee)}</td>
                                    <td>{won(month.net)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>

                    <p className="meta">
                        이 표는 장부입니다. 실제 송금은 서비스 운영자가 따로 처리합니다.
                    </p>
                </>
            )}
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
