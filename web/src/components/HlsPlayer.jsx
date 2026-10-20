import { useEffect, useRef, useState } from 'react'
import Hls from 'hls.js'
import { getAccessToken } from '../api.js'
import { lowLatencySupported, startLowLatency } from '../lowLatency.js'
import { fpsMeterEnabled } from '../perf.js'

/**
 * 라이브 재생 설정. hls.js 기본값은 "조각 3개(≈10초) 뒤"를 따라가서 지연이 크다.
 * 브라우저 방송은 조각이 약 3.4초(Chrome 키프레임 간격)라, 2개 뒤로 줄이면 총 지연이 12~14초에서 7~8초 안팎이 된다.
 * 더 줄이면(1개) 네트워크가 조금만 흔들려도 멈춘다.
 */
const LIVE_CONFIG = {
    liveSyncDurationCount: 2,
    // 이 이상 뒤처지면(조각 5개) 재생 위치를 방송 끝으로 옮긴다. 멈췄다가 이어졌을 때 계속 뒤처지지 않게 한다.
    liveMaxLatencyDurationCount: 5,
    // 조금 뒤처졌을 때는 1.15배속으로 따라잡는다. 눈에 띄지 않는 속도다.
    maxLiveSyncPlaybackRate: 1.15,
    // 지난 영상은 30초만 들고 있어 오래 켜 둔 기기의 메모리를 아낀다.
    backBufferLength: 30,
    // 학교 와이파이처럼 흔들리는 망에서 한두 번 실패했다고 바로 포기하지 않는다.
    manifestLoadingMaxRetry: 6,
    levelLoadingMaxRetry: 6,
    fragLoadingMaxRetry: 6,
}

/** 이 이상 뒤처지면(초) "지금 방송으로" 버튼을 보여 준다. */
const FAR_BEHIND_SECONDS = 12

/**
 * 저지연 재생이 이 브라우저에서 안 되는 것으로 확인되면(코덱·버퍼 오류) 이 페이지에서는 다시 시도하지 않는다.
 * 연결이 잠깐 안 된 경우(방송 준비 중, 네트워크)는 영구 실패로 치지 않는다.
 */
let lowLatencyBroken = false
const PERMANENT_FAILURES = ['codec', 'source-buffer', 'buffer-error', 'append']

/**
 * m3u8 재생. 브라우저가 HLS 를 기본 지원하면(Safari 등) hls.js 없이 재생한다.
 *
 * onTimeUpdate(초) 를 주면 재생 위치가 바뀔 때마다 알려 준다. 다시보기 채팅이 영상과 맞춰 흐르는 데 쓴다.
 * waitingLabel 을 주면 송출이 아직 오지 않는 동안 영상 면 안에 그 문구로 기다리는 중임을 알린다(오류가 아니다).
 * label 은 영상의 접근 이름, children 은 영상 위 왼쪽 위에 얹을 표시(LIVE 배지 등)다.
 * children 이 함수면 { waiting } 을 받아, 송출이 오기 전과 후에 다른 표시를 낼 수 있다.
 */
export default function HlsPlayer({ src, poster, onTimeUpdate, waitingLabel, label, children, live = false, lowLatencyUrl = null }) {
    const videoRef = useRef(null)
    const hlsRef = useRef(null)
    const [behind, setBehind] = useState(false)
    const [stats, setStats] = useState(null)
    const [mode, setMode] = useState('hls')
    const [llStats, setLlStats] = useState(null)
    const [error, setError] = useState(null)
    const [waiting, setWaiting] = useState(true)

    useEffect(() => {
        const video = videoRef.current

        if (!video || !onTimeUpdate) return

        const report = () => onTimeUpdate(video.currentTime)

        // 건너뛰어도(seeked) 바로 맞춰져야 해서 timeupdate 와 함께 듣는다.
        video.addEventListener('timeupdate', report)
        video.addEventListener('seeked', report)

        return () => {
            video.removeEventListener('timeupdate', report)
            video.removeEventListener('seeked', report)
        }
    }, [onTimeUpdate])

    useEffect(() => {
        const video = videoRef.current

        if (!video || !src) return

        setError(null)
        setWaiting(true)
        setMode('hls')
        setLlStats(null)

        const play = () => {
            video.play().catch(() => {
                // 자동 재생 정책에 막힐 수 있다. 사용자가 직접 재생하면 된다.
            })
        }

        /** HLS 로 재생한다. 저지연이 안 되는 브라우저와, 저지연이 실패했을 때의 이어받기. */
        function startHls() {
            if (Hls.isSupported()) {
                const hls = new Hls(live ? LIVE_CONFIG : {})
                hlsRef.current = hls
                let retryTimer = null
                let stalls = 0

                // 라이브가 얼마나 뒤처졌는지 2초마다 본다. ?fps=1 이면 재생 상태도 계기판으로 보여 준다.
                const onWaiting = () => (stalls += 1)
                video.addEventListener('waiting', onWaiting)

                const statsTimer = setInterval(() => {
                    if (live) setBehind((hls.latency ?? 0) > FAR_BEHIND_SECONDS)

                    if (fpsMeterEnabled()) {
                        const quality = video.getVideoPlaybackQuality?.()
                        const ahead = video.buffered.length ? video.buffered.end(video.buffered.length - 1) - video.currentTime : 0

                        setStats({
                            latency: live && hls.latency != null ? hls.latency.toFixed(1) : null,
                            ahead: ahead.toFixed(1),
                            mbps: hls.bandwidthEstimate ? (hls.bandwidthEstimate / 1e6).toFixed(1) : '?',
                            level: hls.levels?.[hls.currentLevel]?.bitrate ? Math.round(hls.levels[hls.currentLevel].bitrate / 1000) : '?',
                            dropped: quality?.droppedVideoFrames ?? 0,
                            stalls,
                        })
                    }
                }, 2000)

                // 끊겼다가 이어져 다시 재생되면 기다리는 표시를 걷는다.
                const onPlaying = () => setWaiting(false)
                video.addEventListener('playing', onPlaying)

                hls.loadSource(src)
                hls.attachMedia(video)

                hls.on(Hls.Events.MANIFEST_PARSED, () => {
                    setError(null)
                    setWaiting(false)
                    play()
                })

                hls.on(Hls.Events.ERROR, (_event, data) => {
                    if (!data.fatal) return

                    if (data.type === Hls.ErrorTypes.NETWORK_ERROR) {
                        // 송출이 아직 오지 않았을 뿐 고장이 아니다. 오류가 아니라 기다리는 상태로 보여 준다.
                        setWaiting(true)

                        // 방송이 막 시작돼 재생목록이 아직 만들어지기 전이면 처음 읽기가 실패한다. 잠시 뒤 다시 읽는다.
                        // 재생목록을 못 읽은 경우는 처음부터 다시 불러오고, 재생 도중 끊긴 경우는 이어서 받는다.
                        const manifest =
                            data.details === Hls.ErrorDetails.MANIFEST_LOAD_ERROR ||
                            data.details === Hls.ErrorDetails.MANIFEST_LOAD_TIMEOUT

                        clearTimeout(retryTimer)
                        retryTimer = setTimeout(() => (manifest ? hls.loadSource(src) : hls.startLoad()), 3000)
                    } else if (data.type === Hls.ErrorTypes.MEDIA_ERROR) {
                        hls.recoverMediaError()
                    } else {
                        setError('재생 중 오류가 발생했습니다.')
                        hls.destroy()
                    }
                })

                return () => {
                    clearTimeout(retryTimer)
                    clearInterval(statsTimer)
                    video.removeEventListener('playing', onPlaying)
                    video.removeEventListener('waiting', onWaiting)
                    hlsRef.current = null
                    hls.destroy()
                }
            }

            if (video.canPlayType('application/vnd.apple.mpegurl')) {
                const ready = () => {
                    setWaiting(false)
                    play()
                }

                video.src = src
                video.addEventListener('loadedmetadata', ready)

                return () => {
                    video.removeEventListener('loadedmetadata', ready)
                    video.removeAttribute('src')
                    video.load()
                }
            }

            setError('이 브라우저는 HLS 재생을 지원하지 않습니다.')
            return undefined
        }

        // 브라우저 방송은 WebSocket + MSE 로 0.1초 단위 조각을 바로 받아 1~2초 지연으로 재생한다.
        // 안 되면(미지원 브라우저·연결 실패) 위의 HLS 로 이어서 본다.
        if (live && lowLatencyUrl && lowLatencySupported() && !lowLatencyBroken) {
            let hlsCleanup

            const lowLatency = startLowLatency(video, {
                url: lowLatencyUrl,
                token: getAccessToken(),
                onReady: () => {
                    setWaiting(false)
                    setError(null)
                    setMode('ll')
                },
                onStats: (next) => {
                    if (fpsMeterEnabled()) setLlStats(next)
                },
                onEnded: () => setWaiting(true),
                onFail: (reason) => {
                    if (PERMANENT_FAILURES.includes(reason)) lowLatencyBroken = true

                    video.removeAttribute('src')
                    video.load()
                    setMode('hls')
                    setLlStats(null)
                    hlsCleanup = startHls()
                },
            })

            return () => {
                lowLatency.stop()
                hlsCleanup?.()
            }
        }

        return startHls()
    }, [src, live, lowLatencyUrl])

    /** 방송 끝(지금)으로 건너뛴다. 멈춰 있다가 돌아온 시청자가 한참 지난 장면을 보지 않게 한다. */
    function jumpToLive() {
        const hls = hlsRef.current
        const video = videoRef.current

        if (hls?.liveSyncPosition != null && video) {
            video.currentTime = hls.liveSyncPosition
            video.play().catch(() => {})
            setBehind(false)
        }
    }

    return (
        <>
            <div className="player-box">
                <video
                    ref={videoRef}
                    className="player"
                    controls
                    autoPlay
                    muted
                    playsInline
                    poster={poster}
                    aria-label={label}
                />

                {children && (
                    <div className="player-box__badges">
                        {typeof children === 'function' ? children({ waiting: waiting && !error }) : children}
                    </div>
                )}

                {mode === 'hls' && live && behind && !waiting && (
                    <button type="button" className="player-box__jump" onClick={jumpToLive}>
                        지금 방송으로
                    </button>
                )}

                {mode === 'll' && llStats && (
                    <div className="fps-meter player-box__stats" aria-hidden="true">
                        <b>저지연 · 지연 {llStats.lag.toFixed(1)}s</b> · 속도 {llStats.rate}x
                        <br />
                        받은 조각 {llStats.received} · 대기 {llStats.queued}
                    </div>
                )}

                {mode === 'hls' && stats && (
                    <div className="fps-meter player-box__stats" aria-hidden="true">
                        {stats.latency != null && <><b>지연 {stats.latency}s</b> · </>}
                        버퍼 {stats.ahead}s · 대역폭 {stats.mbps}Mbps · 화질 {stats.level}kbps
                        <br />
                        끊김 {stats.stalls}회 · 버린 프레임 {stats.dropped}
                    </div>
                )}

                {waiting && !error && waitingLabel && (
                    <div className="player-box__wait" role="status">
                        <strong>{waitingLabel}</strong>
                        <span>송출이 시작되면 자동으로 이어져요</span>
                        <span className="player-box__dots" aria-hidden="true">
                            <i />
                            <i />
                            <i />
                        </span>
                    </div>
                )}
            </div>

            {error && <p className="error" role="alert">{error}</p>}
        </>
    )
}
