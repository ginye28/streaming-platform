import { useEffect, useRef, useState } from 'react'
import Hls from 'hls.js'
import AdminApp from './admin/AdminApp.jsx'

const HLS_BASE_URL =
  import.meta.env.VITE_HLS_BASE_URL ?? 'http://localhost:8081/hls'

/** ?stream=<이름> 으로 재생할 방송을 고른다. */
function useStreamName() {
  const [name, setName] = useState(
    () => new URLSearchParams(window.location.search).get('stream') ?? 'test'
  )

  useEffect(() => {
    const onPopState = () =>
      setName(new URLSearchParams(window.location.search).get('stream') ?? 'test')

    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])

  return name
}

/** ?view=admin 이면 관리자 화면. 라우터를 들이지 않고 ?stream= 과 같은 방식으로 맞춘다. */
function useView() {
  const read = () => new URLSearchParams(window.location.search).get('view')
  const [view, setView] = useState(read)

  useEffect(() => {
    const onPopState = () => setView(read())

    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])

  return view
}

function Player() {
  const videoRef = useRef(null)
  const streamName = useStreamName()
  const [error, setError] = useState(null)

  useEffect(() => {
    const video = videoRef.current
    if (!video) return

    const videoSrc = `${HLS_BASE_URL}/${encodeURIComponent(streamName)}.m3u8`

    setError(null)

    const play = () => {
      video.play().catch(() => {
        // 브라우저 자동 재생 정책상 막힐 수 있다. 사용자가 직접 재생하면 된다.
      })
    }

    if (Hls.isSupported()) {
      const hls = new Hls()

      hls.loadSource(videoSrc)
      hls.attachMedia(video)
      hls.on(Hls.Events.MANIFEST_PARSED, play)

      hls.on(Hls.Events.ERROR, (_event, data) => {
        if (!data.fatal) return

        // 네트워크/미디어 오류는 한 번 복구를 시도하고, 그 외에는 포기한다.
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

    // Safari 등 HLS 를 기본 지원하는 브라우저
    if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = videoSrc
      video.addEventListener('loadedmetadata', play)

      return () => {
        video.removeEventListener('loadedmetadata', play)
        video.removeAttribute('src')
        video.load()
      }
    }

    setError('이 브라우저는 HLS 재생을 지원하지 않습니다.')
  }, [streamName])

  return (
    <div>
      <h1>Streaming Platform</h1>

      <video
        ref={videoRef}
        controls
        autoPlay
        muted
        playsInline
        style={{ width: '800px', maxWidth: '100%', background: '#000' }}
      />

      {error && <p role="alert">{error}</p>}

      <p>
        <a href="?view=admin">관리자</a>
      </p>
    </div>
  )
}

function App() {
  return useView() === 'admin' ? <AdminApp /> : <Player />
}

export default App
