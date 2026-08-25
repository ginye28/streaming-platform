import { useEffect, useRef, useState } from 'react'
import Hls from 'hls.js'

/** m3u8 재생. 브라우저가 HLS 를 기본 지원하면(Safari 등) hls.js 없이 재생한다. */
export default function HlsPlayer({ src, poster }) {
    const videoRef = useRef(null)
    const [error, setError] = useState(null)

    useEffect(() => {
        const video = videoRef.current

        if (!video || !src) return

        setError(null)

        const play = () => {
            video.play().catch(() => {
                // 자동 재생 정책에 막힐 수 있다. 사용자가 직접 재생하면 된다.
            })
        }

        if (Hls.isSupported()) {
            const hls = new Hls()

            hls.loadSource(src)
            hls.attachMedia(video)
            hls.on(Hls.Events.MANIFEST_PARSED, play)

            hls.on(Hls.Events.ERROR, (_event, data) => {
                if (!data.fatal) return

                if (data.type === Hls.ErrorTypes.NETWORK_ERROR) {
                    setError('방송을 불러오지 못했습니다. 송출 중인지 확인해 주세요.')
                    hls.startLoad()
                } else if (data.type === Hls.ErrorTypes.MEDIA_ERROR) {
                    hls.recoverMediaError()
                } else {
                    setError('재생 중 오류가 발생했습니다.')
                    hls.destroy()
                }
            })

            return () => hls.destroy()
        }

        if (video.canPlayType('application/vnd.apple.mpegurl')) {
            video.src = src
            video.addEventListener('loadedmetadata', play)

            return () => {
                video.removeEventListener('loadedmetadata', play)
                video.removeAttribute('src')
                video.load()
            }
        }

        setError('이 브라우저는 HLS 재생을 지원하지 않습니다.')
    }, [src])

    return (
        <>
            <video ref={videoRef} className="player" controls autoPlay muted playsInline poster={poster} />
            {error && <p className="error" role="alert">{error}</p>}
        </>
    )
}
