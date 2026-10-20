import { assetUrl } from '../assets.js'
import { tintOf } from '../tint.js'

/**
 * 원형 얼굴. 사진이 없으면 이름 첫 글자를 쓰고, 사람마다 고정된 옅은 색조를 준다
 * (같은 사람은 어디서나 같은 색, 이웃한 사람과는 색이 다르게 흩어진다). 색조는 정체성이지 데이터가 아니라 늘 옅다.
 * live 면 방송 중 표시로 빨간 테두리가 붙는다.
 */
export default function Avatar({ src, name = '', size = 36, live = false, seed, className = '' }) {
    const classes = ['avatar', live && 'avatar--live', className].filter(Boolean).join(' ')

    return (
        <span className={classes} style={{ '--s': `${size}px` }} data-tint={tintOf(seed ?? name)} aria-hidden="true">
            {src ? <img src={assetUrl(src)} alt="" loading="lazy" decoding="async" /> : name.slice(0, 1)}
        </span>
    )
}
