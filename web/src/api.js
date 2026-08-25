const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

const ACCESS_TOKEN_KEY = 'sp.accessToken'
const REFRESH_TOKEN_KEY = 'sp.refreshToken'

export function getAccessToken() {
    return localStorage.getItem(ACCESS_TOKEN_KEY)
}

function setTokens({ accessToken, refreshToken }) {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}

function clearTokens() {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
}

/** 서버가 돌려주는 { success, data, message } 봉투를 벗기고, 실패면 message 로 예외를 던진다. */
async function unwrap(response) {
    const body = await response.json().catch(() => null)

    if (!response.ok) {
        throw new Error(body?.message ?? `요청에 실패했습니다. (HTTP ${response.status})`)
    }

    return body?.data
}

function request(path, { method = 'GET', body, token } = {}) {
    return fetch(`${API_BASE_URL}${path}`, {
        method,
        headers: {
            ...(body ? { 'Content-Type': 'application/json' } : {}),
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        ...(body ? { body: JSON.stringify(body) } : {}),
    })
}

/**
 * 인증이 필요한 요청. 액세스 토큰이 만료돼 401 이 오면
 * 리프레시 토큰으로 한 번 재발급받아 같은 요청을 다시 보낸다.
 */
async function authorized(path, options = {}) {
    const response = await request(path, { ...options, token: getAccessToken() })

    if (response.status !== 401) {
        return unwrap(response)
    }

    const refreshed = await tryRefresh()

    if (!refreshed) {
        clearTokens()
        throw new Error('로그인이 만료되었습니다. 다시 로그인해 주세요.')
    }

    return unwrap(await request(path, { ...options, token: getAccessToken() }))
}

async function tryRefresh() {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)

    if (!refreshToken) {
        return false
    }

    const response = await request('/api/auth/refresh', {
        method: 'POST',
        body: { refreshToken },
    })

    if (!response.ok) {
        return false
    }

    const data = await unwrap(response)
    setTokens(data)

    return true
}

export async function login(email, password) {
    const data = await unwrap(
        await request('/api/auth/login', { method: 'POST', body: { email, password } })
    )

    setTokens(data)

    return data
}

export async function logout() {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)

    if (refreshToken) {
        // 서버에서 무효화에 실패해도 로컬 토큰은 반드시 지운다.
        await request('/api/auth/logout', { method: 'POST', body: { refreshToken } }).catch(() => {})
    }

    clearTokens()
}

export const getMe = () => authorized('/api/users/me')

/** 빈 값인 파라미터는 빼고 쿼리스트링을 만든다. */
function query(params) {
    const search = new URLSearchParams(
        Object.entries(params).filter(([, value]) => value !== '' && value != null)
    )

    return search.toString() ? `?${search}` : ''
}

export const getReports = (status, page = 0) =>
    authorized(`/api/admin/reports${query({ status, page })}`)

export const updateReportStatus = (id, status) =>
    authorized(`/api/admin/reports/${id}`, { method: 'PATCH', body: { status } })

/** 카테고리 목록은 공개라 토큰 없이 조회한다. */
export const getCategories = async () => unwrap(await request('/api/categories'))

export const getUsers = (role, page = 0) =>
    authorized(`/api/admin/users${query({ role, page })}`)

export const changeUserRole = (id, role) =>
    authorized(`/api/admin/users/${id}/role`, { method: 'PATCH', body: { role } })

export const createCategory = (name) =>
    authorized('/api/categories', { method: 'POST', body: { name } })
