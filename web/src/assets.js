import { API_BASE_URL } from './api.js'

/** 업로드된 파일 경로(/uploads/..)를 백엔드 절대 주소로 바꾼다. */
export function assetUrl(path) {
    if (!path) return null

    return path.startsWith('http') ? path : `${API_BASE_URL}${path}`
}

/**
 * 사용자가 적은 주소를 링크로 써도 되는지. http(s) 만 된다.
 * 서버가 새 값은 걸러 주지만, 필터가 생기기 전에 저장된 값에 javascript: 같은 주소가 있을 수 있어 화면에서도 거른다.
 */
export function isSafeLink(url) {
    if (!url) return false

    try {
        const { protocol } = new URL(url)

        return protocol === 'https:' || protocol === 'http:'
    } catch {
        return false
    }
}
