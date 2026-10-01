import { useEffect, useRef, useState } from 'react'
import { getNextLive, recordIntroSeen, toggleSubscribe } from '../api.js'
import { assetUrl } from '../assets.js'
import { navigate } from '../router.js'

/** 자기소개 영상이 없는 카드는 이만큼 보여 준 뒤 방송으로 넘어간다. */
const CARD_SECONDS = 8

/**
 * 방송에 처음 들어온 사람에게 "이 사람이 누구인지" 를 먼저 보여 준다.
 *
 * 낯선 방송에 들어가면 잡담 한복판에 떨어져 그대로 나가 버린다.
 * 그 사이에 한 겹을 두되, 광고가 되지 않도록 스킵은 처음부터 열어 둔다.
 *
 * 인트로가 끝나면 알아서 방송으로 넘어간다 — 영상은 끝났을 때, 영상이 없는 카드는 몇 초 뒤.
 * 사람이 누르는 일은 건너뛰기(SKIP)나 다음 방송(PASS)뿐이다.
 *
 * 띄울지 말지는 서버가 정한다(intro.showGate). 여기서는 무엇을 했는지만 알린다.
 */
export default function IntroGate({ intro, liveId, onEnter }) {
    const [subscribed, setSubscribed] = useState(intro.subscribed)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)

    const hasVideo = Boolean(intro.videoUrl)
    const [secondsLeft, setSecondsLeft] = useState(CARD_SECONDS)
    const [paused, setPaused] = useState(false)

    // 영상이 끝나는 것과 타이머가 다 되는 것이 겹쳐도 한 번만 들어가도록 막는다.
    const entered = useRef(false)

    const videoRef = useRef(null)

    // 소리 있는 자동재생은 브라우저가 막을 수 있다(링크로 바로 들어온 경우).
    // 멈춘 채로 있으면 영상이 끝나지 않아 넘어가지도 못하므로, 막히면 소리를 끄고 다시 튼다.
    // 소리는 영상 컨트롤로 켤 수 있다.
    useEffect(() => {
        const video = videoRef.current

        if (!video) return

        video.play().catch(() => {
            video.muted = true
            video.play().catch(() => {
                // 이것도 막히면 사람이 재생하거나 건너뛰면 된다.
            })
        })
    }, [])

    /** 기록에 실패해도 화면은 막지 않는다. 인트로가 다시 뜨는 것뿐이다. */
    async function remember(action) {
        try {
            await recordIntroSeen(intro.channelId, action)
        } catch {
            // 무시한다
        }
    }

    /** 건너뛰든 끝까지 보든, 방송으로 한 번만 들어간다. action 은 무엇을 했는지의 기록이다. */
    async function enterWith(action) {
        if (entered.current) return

        entered.current = true
        setBusy(true)
        await remember(action)
        onEnter()
    }

    // 카드는 1초씩 세다가 0이 되면 넘어간다. 멈추면 세지 않는다.
    useEffect(() => {
        if (hasVideo || paused || busy) return undefined

        const timer = setTimeout(() => {
            if (secondsLeft <= 1) {
                enterWith('WATCHED')
            } else {
                setSecondsLeft((seconds) => seconds - 1)
            }
        }, 1000)

        return () => clearTimeout(timer)
        // enterWith 는 매 렌더 새로 만들어지지만 하는 일은 같다. 다시 거는 조건은 아래가 정한다.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [hasVideo, paused, busy, secondsLeft])

    async function pass() {
        setBusy(true)
        setError(null)
        await remember('PASS')

        const next = await getNextLive(liveId)

        if (next) {
            navigate({ view: 'live', id: next.id })
        } else {
            setError('지금은 더 볼 방송이 없습니다.')
            setBusy(false)
        }
    }

    async function subscribe() {
        // 구독을 누른다는 건 이 화면을 읽고 있다는 뜻이다. 읽는 도중에 넘어가지 않게 멈춘다.
        setPaused(true)

        try {
            const result = await toggleSubscribe(intro.channelId)
            setSubscribed(result.subscribed)
        } catch (e) {
            setError(e.message)
        }
    }

    return (
        <section className="intro">
            <div className="intro__body">
                {intro.videoUrl ? (
                    <video
                        ref={videoRef}
                        className="intro__video"
                        src={assetUrl(intro.videoUrl)}
                        autoPlay
                        playsInline
                        onEnded={() => enterWith('WATCHED')}
                        controls
                    />
                ) : (
                    <IntroCard intro={intro} />
                )}

                {intro.videoUrl && intro.headline && (
                    <p className="intro__headline">{intro.headline}</p>
                )}

                {error && <p className="error">{error}</p>}

                {hasVideo ? (
                    <p className="intro__countdown">자기소개가 끝나면 방송이 시작됩니다.</p>
                ) : (
                    <p className="intro__countdown">
                        {paused
                            ? '멈춰 있습니다. 준비되면 방송 보기를 눌러 주세요.'
                            : `${secondsLeft}초 뒤 방송이 시작됩니다.`}{' '}
                        <button type="button" onClick={() => setPaused(!paused)} disabled={busy}>
                            {paused ? '다시 세기' : '멈추기'}
                        </button>
                    </p>
                )}

                <div className="intro__actions">
                    <button type="button" onClick={() => enterWith('SKIP')} disabled={busy}>
                        건너뛰고 방송 보기
                    </button>
                    <button type="button" onClick={pass} disabled={busy}>
                        다음 방송
                    </button>
                    <button type="button" onClick={subscribe} disabled={busy}>
                        {subscribed ? '구독 중' : '구독'}
                    </button>
                </div>
            </div>
        </section>
    )
}

/** 자기소개 영상이 없는 채널을 위한 대체 화면. 빈손으로 남지 않게 한다. */
function IntroCard({ intro }) {
    return (
        <div className="intro__card">
            {intro.profileImage ? (
                <img src={assetUrl(intro.profileImage)} alt="" className="intro__avatar" />
            ) : (
                <div className="intro__avatar intro__avatar--blank">{intro.nickname[0]}</div>
            )}

            <h2>{intro.nickname}</h2>

            {intro.headline && <p className="intro__headline">{intro.headline}</p>}
            {intro.greeting && <p className="intro__greeting">{intro.greeting}</p>}

            <p className="meta">구독자 {intro.subscriberCount}명</p>
        </div>
    )
}
