import { VideoCard } from './MediaCards.jsx'

/** 영상 카드 목록(홈과 같은 카드 격자). */
export default function StreamList({ streams, empty = '영상이 없습니다.' }) {
    if (!streams || streams.length === 0) {
        return <p className="empty">{empty}</p>
    }

    return (
        <ul className="vgrid">
            {streams.map((stream, i) => (
                <VideoCard key={stream.id} stream={stream} index={i} />
            ))}
        </ul>
    )
}
