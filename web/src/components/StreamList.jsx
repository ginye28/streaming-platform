import { assetUrl } from '../assets.js'
import Link from '../components/Link.jsx'
import { timeAgo } from '../time.js'

/** 영상 카드 목록. showChannel 이 false 면 채널 이름을 뺀다(한 채널의 영상만 모아 보는 화면). */
export default function StreamList({ streams, empty = '영상이 없습니다.', showChannel = true }) {
    if (!streams || streams.length === 0) {
        return <p className="empty">{empty}</p>
    }

    return (
        <ul className="cards">
            {streams.map((stream) => (
                <li key={stream.id} className="card">
                    {/* 제목 링크와 같은 곳으로 가는 중복이라 키보드·스크린리더에서는 뺀다. */}
                    <Link
                        to={{ view: 'stream', id: stream.id }}
                        className="card__thumb"
                        aria-hidden="true"
                        tabIndex={-1}
                    >
                        {stream.thumbnailUrl ? (
                            <img src={assetUrl(stream.thumbnailUrl)} alt="" loading="lazy" decoding="async" />
                        ) : (
                            <div className="card__thumb--blank">썸네일 없음</div>
                        )}
                    </Link>

                    <div className="card__body">
                        <Link to={{ view: 'stream', id: stream.id }} className="card__title">
                            {stream.title}
                        </Link>

                        {showChannel && (
                            <Link to={{ view: 'channel', id: stream.userId }} className="card__meta">
                                {stream.nickname}
                            </Link>
                        )}

                        <p className="card__meta">
                            조회 {stream.viewCount?.toLocaleString('ko-KR')}회
                            {stream.createdAt && ` · ${timeAgo(stream.createdAt)}`}
                            {stream.categoryName && ` · ${stream.categoryName}`}
                        </p>
                    </div>
                </li>
            ))}
        </ul>
    )
}
