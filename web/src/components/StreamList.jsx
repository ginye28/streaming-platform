import { assetUrl } from '../assets.js'
import Link from '../components/Link.jsx'

export default function StreamList({ streams, empty = '영상이 없습니다.' }) {
    if (!streams || streams.length === 0) {
        return <p className="empty">{empty}</p>
    }

    return (
        <ul className="cards">
            {streams.map((stream) => (
                <li key={stream.id} className="card">
                    <Link to={{ view: 'stream', id: stream.id }} className="card__thumb">
                        {stream.thumbnailUrl ? (
                            <img src={assetUrl(stream.thumbnailUrl)} alt="" />
                        ) : (
                            <div className="card__thumb--blank">썸네일 없음</div>
                        )}
                    </Link>

                    <div className="card__body">
                        <Link to={{ view: 'stream', id: stream.id }} className="card__title">
                            {stream.title}
                        </Link>

                        <Link to={{ view: 'channel', id: stream.userId }} className="card__meta">
                            {stream.nickname}
                        </Link>

                        <p className="card__meta">
                            조회 {stream.viewCount} · 좋아요 {stream.likeCount} · 댓글{' '}
                            {stream.commentCount}
                            {stream.categoryName && ` · ${stream.categoryName}`}
                        </p>
                    </div>
                </li>
            ))}
        </ul>
    )
}
