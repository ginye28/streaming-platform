import { assetUrl } from '../assets.js'

/**
 * 이름 옆에 붙는 오시마크. 채팅과 댓글이 같이 쓴다.
 * 유료 구독자는 테두리가 둘러져 일반 구독자와 한눈에 구분된다.
 */
export default function OshiMark({ url, tier }) {
    if (!url) return null

    const paid = tier === 'PAID'
    const label = paid ? '유료 구독자' : '구독자'

    return (
        <img
            className={paid ? 'oshi-mark oshi-mark--paid' : 'oshi-mark'}
            src={assetUrl(url)}
            alt={label}
            title={label}
        />
    )
}
