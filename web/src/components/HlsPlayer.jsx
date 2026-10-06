import { useEffect, useRef, useState } from 'react'
import Hls from 'hls.js'

/**
 * m3u8 재생. 브라우저가 HLS 를 기본 지원하면(Safari 등) hls.js 없이 재생한다.
 *
 * onTimeUpdate(초) 를 주면 재생 위치가 바뀔 때마다 알려 준다. 다시보기 채팅이 영상과 맞춰 흐르는 데 쓴다.
 * waitingLabel 을 주면 송출이 아직 오지 않는 동안 영상 면 안에 그 문구로 기다리는 중임을 알린다(오류가 아니다).
 * label 은 영상의 접근 이름, children 은 영상 위 왼쪽 위에 얹을 표시(LIVE 배지 등)다.
 * children 이 함수면 { waiting } 을 받아, 송출이 오기 전과 후에 다른 표시를 낼 수 있다.
 */
export default function HlsPlayer({ src, poster, onTimeUpdate, waitingLabel, label, children }) {
    const videoRef = useRef(null)
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

        const play = () => {
            video.play().catch(() => {
                // 자동 재생 정책에 막힐 수 있다. 사용자가 직접 재생하면 된다.
            })
        }

        if (Hls.isSupported()) {
            const hls = new Hls()
            let retryTimer = null

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
                video.removeEventListener('playing', onPlaying)
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
    }, [src])

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
