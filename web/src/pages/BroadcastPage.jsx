import { useEffect, useRef, useState } from 'react'
import { getLiveSetting } from '../api.js'
import { AUDIENCE_LABEL } from '../audience.js'
import Link from '../components/Link.jsx'
import { openCapture, pickMimeType, startBroadcast } from '../broadcast.js'
import { loginTo, navigate, setLeaveGuard, toSearch } from '../router.js'
import { useAsyncData } from '../useAsyncData.js'
import { useAuth } from '../useAuth.js'
import { usePageTitle } from '../usePageTitle.js'
import { useSlow } from '../useSlow.js'

const SOURCES = [
    { value: 'screen', label: '화면 공유', hint: '컴퓨터 화면이나 창, 탭' },
    { value: 'camera', label: '카메라', hint: '이 기기의 카메라' },
]

// 서버가 응답하지 않을 때 영영 기다리지 않게 하는 한도. 무료 서버가 깨어나는 데 최대 1분 반쯤 걸린다.
const CONNECT_TIMEOUT_MS = 90_000

function formatDuration(seconds) {
    const total = Math.floor(seconds)
    const m = String(Math.floor(total / 60)).padStart(2, '0')
    const s = String(total % 60).padStart(2, '0')

    return `${m}:${s}`
}

/** 브라우저·장치가 던진 오류를 사람이 읽을 말로. 영어 원문은 보이지 않는다. */
function captureMessage(e, source) {
    if (e?.name === 'NotAllowedError') {
        return source === 'screen'
            ? '화면 공유를 시작하지 못했어요. 공유할 화면을 고르지 않았거나 허용되지 않았어요. 다시 시작해서 화면을 골라 주세요.'
            : '카메라 사용이 허용되지 않았어요. 주소창 왼쪽의 자물쇠에서 카메라를 허용한 뒤 다시 시작해 주세요.'
    }

    if (e?.name === 'NotFoundError') {
        return '사용할 수 있는 카메라를 찾지 못했어요.'
    }

    return '방송을 시작하지 못했어요. 잠시 뒤에 다시 시도해 주세요.'
}

/**
 * 브라우저에서 바로 방송하는 화면. OBS 없이 화면 공유나 카메라를 서버로 보낸다.
 * 보낸 영상은 서버가 HLS 로 바꿔 시청자에게 내려 준다. (다시보기는 남지 않는다)
 */
export default function BroadcastPage() {
    const { me } = useAuth()

    const canShareScreen = typeof navigator.mediaDevices?.getDisplayMedia === 'function'

    const [source, setSource] = useState(canShareScreen ? 'screen' : 'camera')
    const [withMic, setWithMic] = useState(true)
    const [phase, setPhase] = useState('idle') // idle | connecting | live
    const [liveId, setLiveId] = useState(null)
    const [error, setError] = useState(null)
    const [stats, setStats] = useState(null)
    const [audioInfo, setAudioInfo] = useState(null)
    // 서버에 연결을 건 뒤에야 "취소"가 의미가 있다. (그 전은 화면 선택 창이 열려 있는 단계)
    const [cancellable, setCancellable] = useState(false)
    // 끝낼지, 다른 화면으로 나갈지 묻는 인라인 확인. null | { leaveTo?: 이동할 곳 }
    const [confirming, setConfirming] = useState(null)
    // 방송이 끝난 뒤 한 번 보여 주는 요약.
    const [ended, setEnded] = useState(null)
    const [copied, setCopied] = useState(false)

    const previewRef = useRef(null)
    const streamRef = useRef(null)
    const controlRef = useRef(null)
    const startRef = useRef(null)
    const phaseRef = useRef('idle')
    const secondsRef = useRef(0)
    const wasLiveRef = useRef(false)
    const leavingRef = useRef(false)

    const supported = pickMimeType() !== null
    const busy = phase !== 'idle'
    const wake = useSlow(phase === 'connecting')

    usePageTitle(phase === 'live' ? '● 방송 중' : '브라우저로 방송하기')

    // 이 요청이 무료 서버를 미리 깨우는 역할도 한다. 방송을 누르기 전에 서버가 일어난다.
    const { data: setting, error: settingError, loading: settingLoading } = useAsyncData(
        () => (me ? getLiveSetting() : Promise.resolve(null)),
        [me?.id]
    )

    function releaseCapture() {
        streamRef.current?.getTracks().forEach((track) => track.stop())
        streamRef.current = null

        if (previewRef.current) previewRef.current.srcObject = null
    }

    // 화면을 떠나면 방송도 끝낸다.
    useEffect(() => {
        return () => {
            controlRef.current?.stop()
            releaseCapture()
        }
    }, [])

    // 방송 중에 탭을 닫거나 새로고침하면 브라우저가 한 번 묻는다.
    useEffect(() => {
        phaseRef.current = phase

        if (phase === 'idle') return

        function warn(event) {
            event.preventDefault()
        }

        window.addEventListener('beforeunload', warn)

        return () => window.removeEventListener('beforeunload', warn)
    }, [phase])

    // 사이트 안의 링크·버튼으로 나가려 하면, 막고 그 자리에서 "방송이 끝나요" 를 묻는다.
    useEffect(() => {
        return setLeaveGuard((to) => {
            if (leavingRef.current || phaseRef.current === 'idle') return true

            setConfirming({ leaveTo: to })

            return false
        })
    }, [])

    // 연결이 끝내 안 되면 영영 기다리지 않는다.
    useEffect(() => {
        if (phase !== 'connecting') return

        const timer = setTimeout(() => {
            controlRef.current?.stop()
            setError('방송 서버가 응답하지 않아요. 잠시 뒤에 다시 시도해 주세요.')
        }, CONNECT_TIMEOUT_MS)

        return () => clearTimeout(timer)
    }, [phase])

    // 실패하면 다시 누를 버튼으로 포커스를 돌려 준다. (버튼이 바뀌며 포커스가 body 로 떨어지는 걸 막는다)
    useEffect(() => {
        if (error) startRef.current?.focus()
    }, [error])

    function changeSource(value) {
        setSource(value)
        setError(null)
    }

    async function handleStart() {
        setError(null)
        setEnded(null)
        setStats(null)
        setAudioInfo(null)
        setConfirming(null)
        setCancellable(false)
        secondsRef.current = 0
        wasLiveRef.current = false
        setPhase('connecting')

        try {
            // 화면 공유 창은 이 클릭 직후에 열어야 하므로, 서버에 연결하기 전에 먼저 잡는다.
            const { stream, audio, micError } = await openCapture({ source, withMic })

            setAudioInfo({ audio, micError })

            streamRef.current = stream
            previewRef.current.srcObject = stream

            controlRef.current = await startBroadcast(stream, {
                onStarted: (id) => {
                    wasLiveRef.current = true
                    setLiveId(id)
                    setPhase('live')
                },
                onStats: (next) => {
                    secondsRef.current = next.seconds
                    setStats(next)
                },
                onError: setError,
                onClosed: (reason) => {
                    controlRef.current = null
                    releaseCapture()
                    setPhase('idle')
                    setLiveId(null)
                    setAudioInfo(null)
                    setConfirming(null)
                    setCancellable(false)

                    if (wasLiveRef.current) {
                        setEnded({ seconds: secondsRef.current, reason })
                    }
                },
            })

            setCancellable(true)
        } catch (e) {
            releaseCapture()
            setPhase('idle')
            setError(captureMessage(e, source))
        }
    }

    function handleStop() {
        controlRef.current?.stop()
    }

    function handleLeave() {
        const to = confirming?.leaveTo

        leavingRef.current = true
        controlRef.current?.stop()

        if (to) navigate(to)
    }

    async function handleCopy() {
        const url = `${window.location.origin}${window.location.pathname}${toSearch({ view: 'live', id: liveId })}`

        try {
            await navigator.clipboard.writeText(url)
            setCopied(true)
            setTimeout(() => setCopied(false), 2500)
        } catch {
            setError('링크를 복사하지 못했어요. "내 방송 새 탭으로 열기"로 열어 주소창에서 복사해 주세요.')
        }
    }

    if (!me) {
        return (
            <section className="narrow broadcast">
                <h1>브라우저로 방송하기</h1>
                <p className="upload__lede">OBS 없이 화면이나 카메라를 바로 방송할 수 있어요.</p>
                <p className="meta">방송하려면 로그인이 필요해요.</p>
                <Link to={loginTo()} className="cta">
                    로그인하고 방송하기
                </Link>
            </section>
        )
    }

    return (
        <section className="broadcast">
            <h1>브라우저로 방송하기</h1>

            <p className="upload__lede">
                OBS 없이 화면 공유나 카메라를 바로 보내요. 시청자에게는 6~15초쯤 늦게 보이고, 방송이
                끝나면 다시보기는 남지 않아요. 방송은 동시에 하나만 켤 수 있고, 이 탭을 닫으면 끝나요.
            </p>

            {!supported && (
                <p className="notice" role="status">
                    이 브라우저에서는 방송할 수 없어요. 컴퓨터의 Chrome 또는 Edge 에서 열어 주세요.
                    (영상을 H.264 로 보내야 하는데 이 브라우저는 그렇게 녹화하지 못해요)
                </p>
            )}

            {/* 시작 전에 시청자에게 어떻게 나가는지 보여 준다. 값은 내 계정의 "다음 방송 정보". */}
            <div className="broadcast__setup">
                {settingLoading ? (
                    <p className="meta">방송 정보를 불러오는 중…</p>
                ) : settingError || !setting ? (
                    <p className="meta">
                        방송 정보를 불러오지 못했어요. 서버가 깨어나는 중일 수 있어요.{' '}
                        <Link to={{ view: 'me' }}>내 계정에서 확인</Link>
                    </p>
                ) : (
                    <>
                        <p className="broadcast__setup-title">
                            {setting.title || '제목이 아직 없어요'}
                        </p>
                        <p className="meta">
                            시청 {AUDIENCE_LABEL[setting.audience] ?? '누구나'} · 채팅{' '}
                            {AUDIENCE_LABEL[setting.chatAudience] ?? '누구나'}
                            {' · '}
                            <Link to={{ view: 'me' }}>
                                {setting.title ? '바꾸기' : '내 계정에서 정하기'}
                            </Link>
                        </p>
                    </>
                )}
            </div>

            <fieldset className="broadcast__source" disabled={busy}>
                <legend>방송할 것</legend>

                {SOURCES.map((option) => {
                    const unavailable = option.value === 'screen' && !canShareScreen

                    return (
                        <label
                            key={option.value}
                            className={`broadcast__choice${source === option.value ? ' broadcast__choice--on' : ''}`}
                        >
                            <input
                                type="radio"
                                name="source"
                                value={option.value}
                                checked={source === option.value}
                                onChange={() => changeSource(option.value)}
                                disabled={unavailable}
                            />
                            <span>
                                <strong>{option.label}</strong>
                                <small>
                                    {unavailable
                                        ? '이 기기에서는 화면 공유를 쓸 수 없어요'
                                        : option.hint}
                                </small>
                            </span>
                        </label>
                    )
                })}

                <label className="broadcast__mic">
                    <input
                        type="checkbox"
                        checked={withMic}
                        onChange={(e) => setWithMic(e.target.checked)}
                        aria-describedby="broadcast-mic-hint"
                    />
                    <span>
                        <strong>마이크 소리 포함</strong>
                        <small id="broadcast-mic-hint">
                            화면 공유에는 보통 소리가 안 들어가요. 켜면 마이크 허용을 물어봐요.
                        </small>
                    </span>
                </label>
            </fieldset>

            {error && (
                <p className="error" role="alert">
                    {error}
                </p>
            )}

            {phase !== 'idle' && audioInfo?.audio === 'none' && (
                <p className="error" role="alert">
                    소리 없이 방송하고 있어요.{' '}
                    {audioInfo.micError === 'denied'
                        ? '마이크가 허용되지 않았어요. 주소창 왼쪽의 자물쇠에서 마이크를 허용한 뒤 방송을 다시 시작해 주세요.'
                        : audioInfo.micError === 'unavailable'
                          ? '사용할 수 있는 마이크를 찾지 못했어요.'
                          : source === 'screen'
                            ? '화면 공유에는 소리가 따로 들어가지 않아요. "마이크 소리 포함"을 켜거나, 탭을 공유할 때 "탭 오디오도 공유"를 체크해 주세요.'
                            : '마이크 소리 포함을 켜 주세요.'}
                </p>
            )}

            {ended && (
                <p className="notice" role="status">
                    {ended.reason === 'ended'
                        ? '화면 공유가 끝나서 방송도 끝났어요.'
                        : ended.reason === 'closed'
                          ? '서버와의 연결이 끊겨 방송이 끝났어요.'
                          : '방송을 마쳤어요.'}{' '}
                    {ended.seconds >= 1 && `${formatDuration(ended.seconds)} 동안 방송했어요. `}
                    다시보기는 남지 않아요.
                </p>
            )}

            {phase === 'connecting' && (
                <p className="meta" role="status">
                    {wake
                        ? '서버를 깨우는 중이에요. 최대 1분쯤 걸려요. 이 탭은 그대로 두세요.'
                        : '방송을 준비하고 있어요…'}
                </p>
            )}

            {phase === 'live' && (
                <div className="broadcast__live">
                    <p className="broadcast__status" role="status">
                        <span className="broadcast__dot" aria-hidden="true" /> 방송 중
                        {audioInfo && ` · 소리 ${audioInfo.audio === 'none' ? '없음' : '있음'}`}
                    </p>
                    <p className="meta" aria-hidden="true">
                        {stats ? `${formatDuration(stats.seconds)} · 송출 ${stats.kbps} kbps` : '00:00'}
                    </p>
                </div>
            )}

            {confirming ? (
                <div className="broadcast__confirm" role="group" aria-label="방송 끝내기 확인">
                    <p>
                        {confirming.leaveTo
                            ? '지금 나가면 방송이 끝나요.'
                            : '지금 방송을 끝낼까요?'}
                    </p>
                    <button
                        type="button"
                        className="button--danger"
                        onClick={confirming.leaveTo ? handleLeave : handleStop}
                    >
                        {confirming.leaveTo ? '방송 끝내고 나가기' : '방송 끝내기'}
                    </button>
                    <button type="button" onClick={() => setConfirming(null)}>
                        계속 방송하기
                    </button>
                </div>
            ) : (
                <div className="upload__actions">
                    {phase === 'idle' && (
                        <button
                            ref={startRef}
                            type="button"
                            className="button--primary"
                            onClick={handleStart}
                            disabled={!supported}
                        >
                            방송 시작
                        </button>
                    )}

                    {phase === 'connecting' && (
                        <>
                            <button type="button" disabled>
                                연결 중…
                            </button>
                            <button type="button" onClick={handleStop} disabled={!cancellable}>
                                취소
                            </button>
                        </>
                    )}

                    {phase === 'live' && (
                        <>
                            <button type="button" onClick={() => setConfirming({})}>
                                방송 종료
                            </button>
                            <button type="button" onClick={handleCopy}>
                                {copied ? '복사했어요' : '시청 링크 복사'}
                            </button>
                            {liveId && (
                                <a
                                    href={toSearch({ view: 'live', id: liveId })}
                                    target="_blank"
                                    rel="noreferrer"
                                >
                                    내 방송 새 탭으로 열기
                                </a>
                            )}
                        </>
                    )}
                </div>
            )}

            <div className="broadcast__preview">
                <video ref={previewRef} autoPlay muted playsInline aria-label="내 방송 미리보기" />
                {phase === 'idle' && (
                    <p className="empty">
                        방송을 시작하면 여기에 시청자가 보는 내 화면이 나와요.
                    </p>
                )}
            </div>
        </section>
    )
}
