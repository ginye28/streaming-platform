import { assetUrl } from '../assets.js'
import { AUDIENCE_NOTE } from '../audience.js'
import { formatDuration, formatLiveFor, formatViewers, formatViews } from '../format.js'
import { toggleHeart, useHearts } from '../hearts.js'
import { useLiveViewerCount } from '../liveViewers.js'
import { tintOf } from '../tint.js'
import { timeAgo } from '../time.js'
import { toTime, useNow } from '../useNow.js'
import Avatar from './Avatar.jsx'
import Icon from './Icon.jsx'
import Link from './Link.jsx'

/** 16:9 썸네일. 사진이 없으면 사람마다 고정된 옅은 색조 위에 이름 첫 글자를 둔다. */
export function Thumb({ src, name, seed, play = false }) {
    return (
        <span className="thumb" data-tint={tintOf(seed ?? name)}>
            {src ? (
                <img src={assetUrl(src)} alt="" loading="lazy" decoding="async" draggable="false" />
            ) : (
                <span className="thumb__blank">{name?.slice(0, 1)}</span>
            )}
            {play && (
                <span className="thumb__play">
                    <Icon name="play" size={22} filled />
                </span>
            )}
        </span>
    )
}

/**
 * 방송에 하트를 누르면 이 브라우저의 홈 맨 앞에 먼저 올라온다. 서버에는 저장되지 않는다.
 * 방송 하나마다 따로 기억하므로, 방송이 끝나면 하트도 사라진다.
 */
export function HeartButton({ liveId, nickname, className = '' }) {
    const hearts = useHearts()
    const on = hearts.includes(liveId)

    return (
        <button
            type="button"
            className={`heart-btn${on ? ' heart-btn--on' : ''} ${className}`.trim()}
            aria-pressed={on}
            aria-label={`${nickname} 방송 하트${on ? ' 빼기' : ''}`}
            title={on ? '하트 빼기' : '하트를 누르면 이 브라우저의 홈 맨 앞에 먼저 보여요'}
            onClick={() => toggleHeart(liveId)}
        >
            <Icon name="heart" size={20} filled={on} />
        </button>
    )
}

/**
 * 방송 중인 카드. 시청자 수는 WebSocket 으로 밀려 오는 값을 쓰고, 마우스를 올리거나 포커스하면
 * "방송 시간 · 최고 시청"이 썸네일 아래에서 올라온다(터치 화면에서는 글 줄에 같은 내용이 있다).
 */
export function LiveCard({ live, index = 0 }) {
    const now = useNow()
    const viewers = useLiveViewerCount(live.id, live.viewerCount)
    const peak = Math.max(live.peakViewerCount ?? 0, viewers)
    const uptime = formatLiveFor(now - toTime(live.startedAt))
    const note = AUDIENCE_NOTE[live.audience]

    return (
        <li className="vcard" style={{ '--i': index }}>
            <div className="vcard__media">
                <Link
                    to={{ view: 'live', id: live.id }}
                    className="vcard__thumb"
                    tabIndex={-1}
                    aria-hidden="true"
                    draggable="false"
                >
                    <Thumb src={live.thumbnailUrl} name={live.nickname} seed={live.channelId} play />
                    <span className="badge badge--live">LIVE</span>
                    <span className="vcard__progress">
                        {uptime} 방송 · 최고 {peak.toLocaleString('ko-KR')}명
                    </span>
                </Link>
                <HeartButton liveId={live.id} nickname={live.nickname} className="vcard__heart" />
            </div>

            <div className="vcard__meta">
                <Link to={{ view: 'channel', id: live.channelId }} className="vcard__avatar" tabIndex={-1} aria-hidden="true">
                    <Avatar name={live.nickname} src={live.profileImage} size={36} seed={live.channelId} live />
                </Link>
                <div className="vcard__text">
                    <h3 className="vcard__title">
                        <Link to={{ view: 'live', id: live.id }}>{live.title}</Link>
                    </h3>
                    <p className="vcard__channel">
                        <Link to={{ view: 'channel', id: live.channelId }}>{live.nickname}</Link>
                    </p>
                    <p className="vcard__stats num">
                        시청 중 {formatViewers(viewers)} · {uptime}
                        {note && ` · ${note}`}
                    </p>
                </div>
            </div>
        </li>
    )
}

/** 썸네일 오른쪽 아래의 영상 길이(03:45). 길이를 모르는 영상에는 붙이지 않는다. */
export function DurationBadge({ seconds }) {
    const text = formatDuration(seconds)

    return text ? <span className="badge badge--duration num">{text}</span> : null
}

/** 올라온 영상 카드. */
export function VideoCard({ stream, index = 0 }) {
    return (
        <li className="vcard" style={{ '--i': index }}>
            <Link to={{ view: 'stream', id: stream.id }} className="vcard__thumb" tabIndex={-1} aria-hidden="true" draggable="false">
                <Thumb src={stream.thumbnailUrl} name={stream.nickname} seed={stream.userId} play />
                <DurationBadge seconds={stream.durationSeconds} />
            </Link>

            <div className="vcard__meta">
                <Link to={{ view: 'channel', id: stream.userId }} className="vcard__avatar" tabIndex={-1} aria-hidden="true">
                    <Avatar name={stream.nickname} size={36} seed={stream.userId} />
                </Link>
                <div className="vcard__text">
                    <h3 className="vcard__title">
                        <Link to={{ view: 'stream', id: stream.id }}>{stream.title}</Link>
                    </h3>
                    <p className="vcard__channel">
                        <Link to={{ view: 'channel', id: stream.userId }}>{stream.nickname}</Link>
                    </p>
                    <p className="vcard__stats num">
                        {formatViews(stream.viewCount)}
                        {stream.createdAt && ` · ${timeAgo(stream.createdAt)}`}
                    </p>
                </div>
            </div>
        </li>
    )
}

/** 불러오는 동안 카드가 들어올 자리를 같은 크기로 먼저 잡는다(내용이 오면 밀리지 않는다). */
export function CardSkeleton({ index = 0 }) {
    return (
        <li className="vcard vcard--skeleton" aria-hidden="true" style={{ '--i': index }}>
            <div className="vcard__thumb skeleton" />
            <div className="vcard__meta">
                <span className="skeleton skeleton--avatar" />
                <div className="vcard__text">
                    <span className="skeleton skeleton--line" />
                    <span className="skeleton skeleton--line skeleton--short" />
                </div>
            </div>
        </li>
    )
}
