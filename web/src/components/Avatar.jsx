import { assetUrl } from '../assets.js'

/**
 * 나뭇잎 모양 얼굴. 사진이 없으면 이름 첫 글자를 쓴다.
 * 대각선으로 마주 보는 모서리가 같은 반지름이라, 사이트의 다른 잎 모양(썸네일·카드)과 한 식구로 보인다.
 */
export default function Avatar({ src, name = '', size = 30, live = false, className = '' }) {
    const classes = ['c-avatar', live && 'c-avatar--live', className].filter(Boolean).join(' ')

    return (
        <span className={classes} style={{ '--s': `${size}px` }} aria-hidden="true">
            {src ? <img src={assetUrl(src)} alt="" loading="lazy" decoding="async" /> : name.slice(0, 1)}
        </span>
    )
}
