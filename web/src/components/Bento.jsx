import { formatLiveFor, formatViewers, formatViews } from '../format.js'
import { useLiveViewerCount } from '../liveViewers.js'
import { timeAgo } from '../time.js'
import { toTime, useNow } from '../useNow.js'
import Avatar from './Avatar.jsx'
import Link from './Link.jsx'
import { tileSize } from '../bento.js'
import { DurationBadge, Thumb } from './MediaCards.jsx'

/*
 * 벤토 카드. 홈의 격자와 목록 화면(라이브·구독·검색·채널)이 함께 쓴다.
 * 항목은 { kind: 'live' | 'video', key, live | stream } 모양이다.
 */

export function BentoItem({ item, size, index }) {
    return (
        <li className={`bento__item bento__item--${size}`} style={{ '--i': index }}>
            {item.kind === 'live' ? <BentoLive live={item.live} size={size} /> : <BentoVideo stream={item.stream} size={size} />}
        </li>
    )
}

function BentoLive({ live, size }) {
    const now = useNow()
    const viewers = useLiveViewerCount(live.id, live.viewerCount)
    const uptime = formatLiveFor(now - toTime(live.startedAt))

    return (
        <BentoFrame
            size={size}
            tone="live"
            to={{ view: 'live', id: live.id }}
            image={live.thumbnailUrl}
            seed={live.channelId}
            nickname={live.nickname}
            title={live.title}
            badge={<span className="cine-tag cine-tag--live">LIVE</span>}
            channel={{ to: { view: 'channel', id: live.channelId }, nickname: live.nickname, avatar: live.profileImage, seed: live.channelId }}
            stats={`시청 중 ${formatViewers(viewers)} · ${uptime}`}
        />
    )
}

function BentoVideo({ stream, size }) {
    return (
        <BentoFrame
            size={size}
            tone="video"
            to={{ view: 'stream', id: stream.id }}
            image={stream.thumbnailUrl}
            seed={stream.userId}
            nickname={stream.nickname}
            title={stream.title}
            duration={stream.durationSeconds}
            channel={{ to: { view: 'channel', id: stream.userId }, nickname: stream.nickname, seed: stream.userId }}
            stats={`${formatViews(stream.viewCount)}${stream.createdAt ? ` · ${timeAgo(stream.createdAt)}` : ''}`}
        />
    )
}

/** 큰 칸과 가로형은 글이 썸네일 위에 얹히고, 작은 칸은 썸네일 아래에 놓인다. 제목 링크가 카드 전체를 덮는다. */
function BentoFrame({ size, tone, to, image, seed, nickname, title, badge, duration, channel, stats }) {
    return (
        <article className={`bcard bcard--${size} bcard--${tone}`}>
            <div className="bcard__media">
                <Thumb src={image} name={nickname} seed={seed} play />
                {badge && <span className="bcard__badge">{badge}</span>}
                <DurationBadge seconds={duration} />
            </div>

            <div className="bcard__meta">
                <Link to={channel.to} className="bcard__avatar" tabIndex={-1} aria-hidden="true">
                    <Avatar name={channel.nickname} src={channel.avatar} size={size === 'mini' ? 28 : 34} seed={channel.seed} />
                </Link>
                <div className="bcard__text">
                    <h3 className="bcard__title">
                        <Link to={to}>{title}</Link>
                    </h3>
                    <p className="bcard__stats num">
                        <Link to={channel.to} className="bcard__channel">
                            {channel.nickname}
                        </Link>
                        {` · ${stats}`}
                    </p>
                </div>
            </div>
        </article>
    )
}

/** 목록 화면용 격자. uniform 이면 모든 칸이 같은 크기이고, 아니면 첫 칸이 크고 가로형이 섞인다. */
export function BentoList({ items, uniform = false, label }) {
    return (
        <ul className={`bento${uniform ? ' bento--even' : ''}`} aria-label={label}>
            {items.map((item, i) => (
                <BentoItem key={item.key} item={item} size={uniform ? 'mini' : tileSize(i)} index={i} />
            ))}
        </ul>
    )
}
