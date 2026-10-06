import { useState } from 'react'
import { useSlow } from '../useSlow.js'

/**
 * 올려 둔 영상 파일(mp4 등) 재생. 라이브의 HlsPlayer 와 같은 영상 상자 안에서
 * 불러오는 중과 실패를 말없이 두지 않고 영상 면 안에서 알려 준다.
 *
 * 불러오는 데 5초가 넘으면 서버가 잠들어 있다가 깨는 중일 수 있다고 알리고,
 * 실패하면 이유와 "다시 시도" 를 보여 준다.
 */
export default function FilePlayer({ src, poster, label }) {
    const [status, setStatus] = useState('loading')
    const [attempt, setAttempt] = useState(0)
    const slow = useSlow(status === 'loading')

    function retry() {
        setStatus('loading')
        setAttempt((value) => value + 1)
    }

    return (
        <div className="player-box">
            <video
                // key 가 바뀌면 영상 요소를 새로 만들어 처음부터 다시 받는다.
                key={attempt}
                className="player"
                controls
                playsInline
                preload="metadata"
                src={src}
                poster={poster ?? undefined}
                aria-label={label}
                onLoadedMetadata={() => setStatus('ready')}
                onError={() => setStatus('error')}
            />

            {status === 'loading' && (
                <div className="player-box__wait" role="status">
                    <strong>영상을 불러오는 중</strong>
                    {slow && <span>서버를 깨우는 중이에요. 최대 1분쯤 걸려요</span>}
                    <span className="player-box__dots" aria-hidden="true">
                        <i />
                        <i />
                        <i />
                    </span>
                </div>
            )}

            {status === 'error' && (
                <div className="player-box__wait player-box__wait--error" role="alert">
                    <strong>영상을 불러오지 못했어요</strong>
                    <span>파일이 없거나 네트워크가 느릴 수 있어요</span>
                    <button type="button" onClick={retry}>
                        다시 시도
                    </button>
                </div>
            )}
        </div>
    )
}
