import { useState } from 'react'
import { getNextLive, recordIntroSeen, toggleSubscribe } from '../api.js'
import { assetUrl } from '../assets.js'
import { navigate } from '../router.js'

/**
 * 방송에 처음 들어온 사람에게 "이 사람이 누구인지" 를 먼저 보여 준다.
 *
 * 낯선 방송에 들어가면 잡담 한복판에 떨어져 그대로 나가 버린다.
 * 그 사이에 한 겹을 두되, 광고가 되지 않도록 스킵은 처음부터 열어 둔다.
 *
 * 띄울지 말지는 서버가 정한다(intro.showGate). 여기서는 무엇을 눌렀는지만 알린다.
 */
export default function IntroGate({ intro, liveId, onEnter }) {
    const [subscribed, setSubscribed] = useState(intro.subscribed)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)

    /** 기록에 실패해도 화면은 막지 않는다. 인트로가 다시 뜨는 것뿐이다. */
    async function remember(action) {
        try {
            await recordIntroSeen(intro.channelId, action)
        } catch {
            // 무시한다
        }
    }

    async function enter() {
        setBusy(true)
        await remember('SKIP')
        onEnter()
    }

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
                        className="intro__video"
                        src={assetUrl(intro.videoUrl)}
                        autoPlay
                        playsInline
                        onEnded={() => remember('WATCHED')}
                        controls
                    />
                ) : (
                    <IntroCard intro={intro} />
                )}

                {intro.videoUrl && intro.headline && (
                    <p className="intro__headline">{intro.headline}</p>
                )}

                {error && <p className="error">{error}</p>}

                <div className="intro__actions">
                    <button type="button" onClick={enter} disabled={busy}>
                        방송 보기
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
