import { getChannelStreams } from '../api.js'
import { assetUrl } from '../assets.js'
import { timeAgo } from '../time.js'
import { useAsyncData } from '../useAsyncData.js'
import Link from './Link.jsx'

/**
 * 영상을 다 본 뒤에 갈 곳. 같은 채널의 다른 영상을 보여 준다.
 * 지금 보는 영상은 빼고, 없으면 홈으로 가는 길을 준다. 불러오지 못하면(보조 내용이라) 그냥 숨긴다.
 */
export default function MoreStreams({ channelId, currentId, nickname }) {
    const { data, loading } = useAsyncData(() => getChannelStreams(channelId, 0), [channelId])

    if (loading || !data) return null

    const others = data.content.filter((stream) => String(stream.id) !== String(currentId)).slice(0, 6)

    return (
        <aside className="more" aria-labelledby="more-title">
            <h2 id="more-title" className="more__title">
                {nickname}님의 다른 영상
            </h2>

            {others.length === 0 ? (
                <p className="meta">
                    이 채널의 다른 영상이 아직 없어요.{' '}
                    <Link to={{ view: 'home' }} className="empty__link">
                        홈에서 다른 영상 둘러보기
                    </Link>
                </p>
            ) : (
                <ul className="more__list">
                    {others.map((stream) => (
                        <li key={stream.id} className="more__item">
                            <Link to={{ view: 'stream', id: stream.id }} className="more__link" draggable="false">
                                <span className="more__thumb">
                                    {stream.thumbnailUrl ? (
                                        <img
                                            src={assetUrl(stream.thumbnailUrl)}
                                            alt=""
                                            loading="lazy"
                                            decoding="async"
                                            draggable="false"
                                        />
                                    ) : (
                                        <span className="more__blank">썸네일 없음</span>
                                    )}
                                </span>

                                <span className="more__text">
                                    <span className="more__name">{stream.title}</span>
                                    <span className="more__meta">
                                        조회 {stream.viewCount?.toLocaleString('ko-KR')}
                                        {stream.createdAt && ` · ${timeAgo(stream.createdAt)}`}
                                    </span>
                                </span>
                            </Link>
                        </li>
                    ))}
                </ul>
            )}
        </aside>
    )
}
