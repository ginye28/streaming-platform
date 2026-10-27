import { getMySubscriptions } from '../api.js'
import { useAsyncData } from '../useAsyncData.js'
import { useAuth } from '../useAuth.js'
import { loginTo } from '../router.js'
import Avatar from './Avatar.jsx'
import HomeArt from './HomeArt.jsx'
import Icon from './Icon.jsx'
import Link from './Link.jsx'

/* 홈의 밝은 화면과 시네마(나이트) 화면이 함께 쓰는 조각들: 구독 채널 줄, 방송이 없을 때의 안내, 빈 영상 안내. */

/* ── 구독 채널: 방송 중인 채널이 앞에 오고 빨간 고리가 둘러싼다 ── */
export function SubscribedChannels() {
    const { me } = useAuth()
    const { data } = useAsyncData(() => (me ? getMySubscriptions(0) : Promise.resolve(null)), [me?.id])

    const channels = [...(data?.content ?? [])].sort((a, b) => Number(b.live) - Number(a.live)).slice(0, 18)

    if (!me || channels.length === 0) return null

    return (
        <section className="zone subs" aria-labelledby="subs-title">
            <div className="shead">
                <h2 id="subs-title">구독 채널</h2>
                <Link to={{ view: 'subscribed' }} className="shead__link">
                    전체 보기
                </Link>
            </div>

            <ul className="subs__row">
                {channels.map((channel) => (
                    <li key={channel.id}>
                        <Link to={{ view: 'channel', id: channel.id }} className="subs__item">
                            <Avatar src={channel.profileImage} name={channel.nickname} size={56} seed={channel.id} live={channel.live} />
                            <span className="subs__name">{channel.nickname}</span>
                            <span className={`subs__state${channel.live ? ' subs__state--live' : ''}`}>
                                {channel.live ? 'LIVE' : ' '}
                            </span>
                        </Link>
                    </li>
                ))}
            </ul>
        </section>
    )
}

/** 방송은 없지만 볼 영상은 있을 때의 한 줄 안내. 방송 켜기와 예정 방송으로 가는 길만 남긴다. */
export function NoLiveBar() {
    const { me } = useAuth()

    return (
        <div className="nolive-bar">
            <span className="nolive-bar__icon">
                <Icon name="live" size={22} />
            </span>
            <p>지금 방송 중인 채널이 없어요. 방송이 시작되면 가장 먼저 여기에 올라와요.</p>
            <div className="nolive-bar__actions">
                <Link to={{ view: 'lives' }} className="btn">
                    예정된 방송 보기
                </Link>
                <Link to={me ? { view: 'broadcast' } : loginTo()} className="btn btn--tonal">
                    {me ? '방송하기' : '로그인하고 방송하기'}
                </Link>
            </div>
        </div>
    )
}

/** 방송이 하나도 없을 때. 이유와 다음 행동을 알려 준다. */
export function NoLive() {
    const { me } = useAuth()

    return (
        <div className="hero-empty">
            <div className="hero-empty__text">
                <h3>지금 방송 중인 채널이 없어요</h3>
                <p>방송이 시작되면 가장 먼저 여기에 올라와요. 직접 방송을 켜 보거나, 곧 시작하는 방송이 있는지 확인해 보세요.</p>
                <div className="hero-empty__actions">
                    <Link to={me ? { view: 'broadcast' } : loginTo()} className="btn btn--primary btn--lg">
                        <Icon name="live" size={20} />
                        {me ? '방송하기' : '로그인하고 방송하기'}
                    </Link>
                    <Link to={{ view: 'lives' }} className="btn btn--lg">
                        예정된 방송 보기
                    </Link>
                </div>
            </div>
            <HomeArt className="hero-empty__art" />
        </div>
    )
}

/** 방송이 없는 첫 방문자에게 방송·영상 올리기 길을 세 갈래로 보여 준다. 이 서비스가 하는 일의 설명이다. */
export function StartGuide() {
    const { me } = useAuth()
    const steps = [
        {
            icon: 'live',
            title: '브라우저로 바로 방송',
            text: 'OBS 없이 Chrome·Edge 에서 화면과 소리를 바로 보내요.',
            to: me ? { view: 'broadcast' } : loginTo(),
        },
        {
            icon: 'gauge',
            title: 'OBS 로 방송',
            text: '내 계정에서 스트림 키를 받아 OBS 에 붙여 넣으면 돼요.',
            to: me ? { view: 'me' } : loginTo(),
        },
        {
            icon: 'upload',
            title: '영상 올리기',
            text: '방송이 아니어도 영상 파일을 올려 두고 다시 볼 수 있어요.',
            to: me ? { view: 'upload' } : loginTo(),
        },
    ]

    return (
        <ul className="steps" aria-label="시작하는 방법">
            {steps.map((step, i) => (
                <li key={step.title} style={{ '--i': i }}>
                    <Link to={step.to} className="step">
                        <span className="step__icon">
                            <Icon name={step.icon} size={24} />
                        </span>
                        <span className="step__body">
                            <strong>{step.title}</strong>
                            <span>{step.text}</span>
                        </span>
                        <Icon name="right" size={20} className="step__go" />
                    </Link>
                </li>
            ))}
        </ul>
    )
}

/** 올라온 영상이 없을 때. 비어 있는 이유와 다음 행동을 함께 보여 준다. */
export function EmptyVideos({ filtered, onShowAll }) {
    const { me } = useAuth()

    if (filtered) {
        return (
            <div className="empty-panel">
                <h3>이 카테고리에는 아직 영상이 없어요</h3>
                <p>다른 카테고리를 고르거나 전체 영상을 둘러보세요.</p>
                <div className="empty-panel__actions">
                    <button type="button" className="btn" onClick={onShowAll}>
                        전체 영상 보기
                    </button>
                </div>
            </div>
        )
    }

    return (
        <div className="hero-empty hero-empty--compact">
            <div className="hero-empty__text">
                <h3>아직 올라온 영상이 없어요</h3>
                <p>첫 영상을 올리면 여기에 가장 먼저 보여요.</p>
                <div className="hero-empty__actions">
                    <Link to={me ? { view: 'upload' } : loginTo()} className="btn btn--primary btn--lg">
                        <Icon name="upload" size={20} />
                        {me ? '첫 영상 올리기' : '로그인하고 첫 영상 올리기'}
                    </Link>
                </div>
            </div>
            <HomeArt className="hero-empty__art" />
        </div>
    )
}
