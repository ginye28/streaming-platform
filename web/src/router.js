import { useEffect, useState } from 'react'

/**
 * 쿼리스트링 기반의 최소 라우터. 라우터 라이브러리를 들이지 않고
 * ?view=stream&id=3 형태로 화면을 고른다.
 */
/** 경로로 들어와도 알아보는 화면. 결제사가 /pay-result 처럼 경로 형태로 돌려보내는 경우가 있다. */
const PATH_VIEWS = new Set(['pay-result', 'pay-fail'])

/**
 * 주소를 ?view=… 형태의 쿼리로 바로잡는다.
 *
 * 평소 주소는 ?view=stream&id=3 이다. 다만 결제 복귀 주소는 바깥(결제사·카드사 앱)이 만들어 주므로 두 가지를 더 받아 준다.
 *  - 경로 형태: /pay-result?paymentKey=…  (→ view=pay-result)
 *  - 쿼리가 두 번 붙어 깨진 형태: ?view=pay-result?paymentKey=…&orderId=… (→ 뒤쪽을 쿼리로 되살린다)
 */
function normalizedParams() {
    let search = window.location.search
    const brokenAt = search.indexOf('?', 1)

    if (brokenAt > 0) {
        search = `${search.slice(0, brokenAt)}&${search.slice(brokenAt + 1)}`
    }

    const params = new URLSearchParams(search)

    if (!params.get('view')) {
        const segment = window.location.pathname.replace(/^\/+|\/+$/g, '').split('/').pop()

        if (PATH_VIEWS.has(segment)) {
            params.set('view', segment)
        }
    }

    return params
}

function read() {
    const params = normalizedParams()

    return {
        view: params.get('view') ?? 'home',
        id: params.get('id'),
        keyword: params.get('keyword'),
        next: params.get('next'),
        params,
    }
}

/**
 * 로그인 화면으로 보내되, 로그인이 끝나면 지금 보던 화면으로 돌아오게 한다.
 * 이미 로그인 화면이면 그대로 둔다.
 */
export function loginTo() {
    if (read().view === 'auth') return { view: 'auth' }

    return { view: 'auth', next: normalizedParams().toString() }
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

    const target = { view, id: params.get('id'), keyword: params.get('keyword') }

    // 결제를 마치고 돌아왔는데 로그인이 풀려 있었다면, 로그인한 뒤 승인을 이어서 할 수 있어야 한다.
    if (view === 'pay-result') {
        for (const key of ['paymentKey', 'orderId', 'amount']) {
            target[key] = params.get(key)
        }
    }

    return target
}

const listeners = new Set()

export function toSearch(params) {
    const search = new URLSearchParams(
        Object.entries(params).filter(([, value]) => value !== '' && value != null)
    )

    return `?${search}`
}

// 화면이 "지금 나가면 잃는 것이 있다"(방송 중 등)고 알릴 때 쓰는 이탈 가드.
// fn(이동할 곳)이 false 를 돌려주면 이동하지 않는다. 그 화면이 인라인으로 확인을 묻는다.
let leaveGuard = null

export function setLeaveGuard(fn) {
    leaveGuard = fn

    return () => {
        if (leaveGuard === fn) leaveGuard = null
    }
}

export function navigate(params) {
    if (leaveGuard && leaveGuard(params) === false) return

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
