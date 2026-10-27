import { videoItems } from '../bento.js'
import { BentoList } from './Bento.jsx'

/** 영상 카드 목록(벤토 격자). 첫 칸이 크게 나오고, 영상이 적으면 그만큼만 채운다. */
export default function StreamList({ streams, empty = '영상이 없습니다.', uniform = false }) {
    if (!streams || streams.length === 0) {
        return <p className="empty">{empty}</p>
    }

    return <BentoList items={videoItems(streams)} uniform={uniform} />
}
