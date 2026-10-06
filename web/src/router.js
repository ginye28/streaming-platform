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
        next: params.get('next'),
    }
}

/**
 * 로그인 화면으로 보내되, 로그인이 끝나면 지금 보던 화면으로 돌아오게 한다.
 * 이미 로그인 화면이면 그대로 둔다.
 */
export function loginTo() {
    if (read().view === 'auth') return { view: 'auth' }

    return { view: 'auth', next: window.location.search.slice(1) }
}

/**
 * 로그인 뒤에 돌아갈 곳. 이 사이트 안의 화면(view · id · keyword)만 받고,
 * 그 밖의 값이나 로그인·관리자 화면은 버려서 바깥 주소로 튀는 일이 없게 한다.
 */
export function parseNext(next) {
    if (!next) return null

    const params = new URLSearchParams(next)
    const view = params.get('view')

    if (!view || !/^[a-z-]+$/.test(view) || view === 'auth' || view === 'admin') return null

    return { view, id: params.get('id'), keyword: params.get('keyword') }
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
