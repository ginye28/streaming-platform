import { API_BASE_URL } from './api.js'

/** 업로드된 파일 경로(/uploads/..)를 백엔드 절대 주소로 바꾼다. */
export function assetUrl(path) {
    if (!path) return null

    return path.startsWith('http') ? path : `${API_BASE_URL}${path}`
}
