import { useEffect, useRef, useState } from 'react'
import { useAuth } from '../useAuth.js'
import { openCapture, pickMimeType, startBroadcast } from '../broadcast.js'
import Link from '../components/Link.jsx'
import { loginTo } from '../router.js'

const SOURCES = [
    { value: 'screen', label: '화면 공유' },
    { value: 'camera', label: '카메라' },
]

function formatDuration(seconds) {
    const total = Math.floor(seconds)
    const m = String(Math.floor(total / 60)).padStart(2, '0')
    const s = String(total % 60).padStart(2, '0')

    return `${m}:${s}`
}

/**
 * 브라우저에서 바로 방송하는 화면. OBS 없이 화면 공유나 카메라를 서버로 보낸다.
 * 보낸 영상은 서버가 HLS 로 바꿔 시청자에게 내려 준다. (다시보기는 남지 않는다)
 */
export default function BroadcastPage() {
    const { me } = useAuth()

    const [source, setSource] = useState('screen')
    const [withMic, setWithMic] = useState(true)
    const [phase, setPhase] = useState('idle') // idle | connecting | live
    const [liveId, setLiveId] = useState(null)
    const [error, setError] = useState(null)
    const [stats, setStats] = useState(null)
    const [audioInfo, setAudioInfo] = useState(null)

    const previewRef = useRef(null)
    const streamRef = useRef(null)
    const controlRef = useRef(null)

    const supported = pickMimeType() !== null

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

    async function handleStart() {
        setError(null)
        setStats(null)
        setAudioInfo(null)
        setPhase('connecting')

        try {
            // 화면 공유 창은 이 클릭 직후에 열어야 하므로, 서버에 연결하기 전에 먼저 잡는다.
            const { stream, audio, micError } = await openCapture({ source, withMic })

            setAudioInfo({ audio, micError })

            streamRef.current = stream
            previewRef.current.srcObject = stream

            controlRef.current = await startBroadcast(stream, {
                onStarted: (id) => {
                    setLiveId(id)
                    setPhase('live')
                },
                onStats: setStats,
                onError: setError,
                onClosed: () => {
                    controlRef.current = null
                    releaseCapture()
                    setPhase('idle')
                    setLiveId(null)
                    setAudioInfo(null)
                },
            })
        } catch (e) {
            releaseCapture()
            setPhase('idle')
            setError(
                e?.name === 'NotAllowedError'
                    ? '화면·카메라 사용이 허용되지 않았어요. 브라우저 설정에서 허용해 주세요.'
                    : (e?.message ?? '방송을 시작하지 못했어요.')
            )
        }
    }

    function handleStop() {
        controlRef.current?.stop()
    }

    if (!me) {
        return (
            <section>
                <h2>브라우저로 방송하기</h2>
                <p className="empty">
                    방송하려면 <Link to={loginTo()}>로그인</Link>이 필요합니다.
                </p>
            </section>
        )
    }

    const busy = phase !== 'idle'

    return (
        <section className="broadcast">
            <h2>브라우저로 방송하기</h2>

            <p className="meta">
                OBS 없이 화면 공유나 카메라를 바로 보냅니다. 제목과 공개 대상은 내 계정의 &ldquo;다음 방송
                정보&rdquo;가 쓰입니다. 이 방식은 다시보기가 남지 않습니다.
            </p>

            {!supported && (
                <p className="error">
                    이 브라우저는 영상을 H.264 로 보낼 수 없습니다. Chrome 또는 Edge 에서 열어 주세요.
                </p>
            )}

            {error && <p className="error">{error}</p>}

            <div className="broadcast__preview">
                <video ref={previewRef} autoPlay muted playsInline />
                {phase === 'idle' && <p className="empty">방송을 시작하면 여기에 내 화면이 보입니다.</p>}
            </div>

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

            {phase === 'live' && (
                <p className="broadcast__status" role="status">
                    <span className="broadcast__dot" aria-hidden="true" /> 방송 중
                    {stats && ` · ${formatDuration(stats.seconds)} · ${stats.kbps} kbps`}
                    {audioInfo && ` · 소리 ${audioInfo.audio === 'none' ? '없음' : '있음'}`}
                </p>
            )}

            <div className="toolbar">
                {SOURCES.map((option) => (
                    <label key={option.value}>
                        <input
                            type="radio"
                            name="source"
                            value={option.value}
                            checked={source === option.value}
                            onChange={() => setSource(option.value)}
                            disabled={busy}
                        />{' '}
                        {option.label}
                    </label>
                ))}

                <label>
                    <input
                        type="checkbox"
                        checked={withMic}
                        onChange={(e) => setWithMic(e.target.checked)}
                        disabled={busy}
                    />{' '}
                    마이크 소리 포함
                </label>
                <span className="meta">
                    (화면 공유에는 보통 소리가 안 들어가서, 켜면 마이크 허용을 물어봐요)
                </span>
            </div>

            <div className="toolbar">
                {phase === 'idle' && (
                    <button onClick={handleStart} disabled={!supported}>
                        방송 시작
                    </button>
                )}

                {phase === 'connecting' && <button disabled>연결 중…</button>}

                {phase === 'live' && (
                    <>
                        <button onClick={handleStop}>방송 종료</button>
                        {liveId && <Link to={{ view: 'live', id: liveId }}>내 방송 화면 보기</Link>}
                    </>
                )}
            </div>
        </section>
    )
}
