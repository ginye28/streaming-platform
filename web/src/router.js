import { useEffect, useState } from 'react'

/**
 * 쿼리스트링 기반의 최소 라우터. 라우터 라이브러리를 들이지 않고
 * ?view=stream&id=3 형태로 화면을 고른다.
 */
function read() {
    const params = new URLSearchParams(window.location.search)

    return {
        view: params.get('view') ?? 'home',
        id: params.get('id'),
        keyword: params.get('keyword'),
    }
}

const listeners = new Set()

export function toSearch(params) {
    const search = new URLSearchParams(
        Object.entries(params).filter(([, value]) => value !== '' && value != null)
    )

    return `?${search}`
}

export function navigate(params) {
    window.history.pushState({}, '', toSearch(params))
    listeners.forEach((listener) => listener())
    window.scrollTo(0, 0)
}

export function useRoute() {
    const [route, setRoute] = useState(read)

    useEffect(() => {
        const update = () => setRoute(read())

        listeners.add(update)
        window.addEventListener('popstate', update)

        return () => {
            listeners.delete(update)
            window.removeEventListener('popstate', update)
        }
    }, [])

    return route
}
