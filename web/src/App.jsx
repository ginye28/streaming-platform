import { useEffect, useRef } from 'react'
import Hls from 'hls.js'

function App() {
  const videoRef = useRef(null)

  useEffect(() => {
    const video = videoRef.current
    const videoSrc = 'http://localhost:8081/hls/test.m3u8'

    if (!video) return

    if (Hls.isSupported()) {
      const hls = new Hls()

      hls.loadSource(videoSrc)
      hls.attachMedia(video)

      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        video.play().catch(() => {
          console.log('자동 재생이 차단되었습니다.')
        })
      })

      return () => {
        hls.destroy()
      }
    }

    if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = videoSrc

      video.addEventListener('loadedmetadata', () => {
        video.play().catch(() => {
          console.log('자동 재생이 차단되었습니다.')
        })
      })
    }
  }, [])

  return (
    <div>
      <h1>Streaming Platform</h1>

      <video
        ref={videoRef}
        controls
        autoPlay
        muted
        style={{ width: '800px', maxWidth: '100%' }}
      />
    </div>
  )
}

export default App