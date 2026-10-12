import { useState } from 'react'

/**
 * 서버에 올린 그림을 보여 주는 <img>. 불러오지 못하면 깨진 그림과 대체 글자를 남기지 않고
 * 아무것도 그리지 않는다(fallback 이 있으면 그것을 그린다).
 * 이름 옆의 작은 표식이 깨져서 글자가 삐져나오는 것을 막으려는 것이다.
 */
export default function AssetImage({ src, alt, fallback = null, ...props }) {
    // 주소가 바뀌면 다시 시도해야 하므로, 실패한 주소 자체를 기억한다.
    const [failedSrc, setFailedSrc] = useState(null)

    if (!src) return fallback

    if (failedSrc === src) return fallback

    return <img src={src} alt={alt} onError={() => setFailedSrc(src)} {...props} />
}
