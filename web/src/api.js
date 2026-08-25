export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

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

function request(path, { method = 'GET', body, token, formData } = {}) {
    return fetch(`${API_BASE_URL}${path}`, {
        method,
        headers: {
            // FormData 는 브라우저가 boundary 를 붙여야 해서 Content-Type 을 직접 넣으면 안 된다.
            ...(body ? { 'Content-Type': 'application/json' } : {}),
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        ...(body ? { body: JSON.stringify(body) } : {}),
        ...(formData ? { body: formData } : {}),
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

/** 로그인했으면 토큰을 붙이고, 아니면 그대로 보낸다. likedByMe 처럼 로그인 여부로 값이 달라지는 공개 API 용. */
async function maybeAuthorized(path) {
    const token = getAccessToken()

    return token ? authorized(path) : unwrap(await request(path))
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

    setTokens(await unwrap(response))

    return true
}

/** 빈 값인 파라미터는 빼고 쿼리스트링을 만든다. */
function query(params) {
    const search = new URLSearchParams(
        Object.entries(params).filter(([, value]) => value !== '' && value != null)
    )

    return search.toString() ? `?${search}` : ''
}

// ---- 인증 ----

export async function login(email, password) {
    const data = await unwrap(
        await request('/api/auth/login', { method: 'POST', body: { email, password } })
    )

    setTokens(data)

    return data
}

export async function signup(email, password, nickname) {
    return unwrap(
        await request('/api/auth/signup', {
            method: 'POST',
            body: { email, password, nickname },
        })
    )
}

export async function logout() {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)

    if (refreshToken) {
        // 서버에서 무효화에 실패해도 로컬 토큰은 반드시 지운다.
        await request('/api/auth/logout', { method: 'POST', body: { refreshToken } }).catch(() => {})
    }

    clearTokens()
}

// ---- 내 계정 ----

export const getMe = () => authorized('/api/users/me')

export const updateProfile = (nickname, profileImage) =>
    authorized('/api/users/me', { method: 'PATCH', body: { nickname, profileImage } })

export const changePassword = (currentPassword, newPassword) =>
    authorized('/api/users/me/password', {
        method: 'PUT',
        body: { currentPassword, newPassword },
    })

export const getStreamKey = () => authorized('/api/users/stream-key')

export const regenerateStreamKey = () =>
    authorized('/api/users/stream-key/regenerate', { method: 'POST' })

export const getMySubscriptions = (page = 0) =>
    authorized(`/api/users/me/subscriptions${query({ page })}`)

export const getMyBlocks = (page = 0) => authorized(`/api/users/me/blocks${query({ page })}`)

export const toggleBlock = (userId) =>
    authorized(`/api/users/${userId}/block`, { method: 'POST' })

// ---- 영상 ----

export const getStreams = (categoryId, page = 0) =>
    maybeAuthorized(`/api/streams${query({ categoryId, page })}`)

export const searchStreams = (keyword, page = 0) =>
    maybeAuthorized(`/api/streams/search${query({ keyword, page })}`)

export const getPopularStreams = () => maybeAuthorized('/api/streams/popular')

export const getLatestStreams = () => maybeAuthorized('/api/streams/latest')

export const getSubscribedFeed = (page = 0) =>
    authorized(`/api/streams/subscribed${query({ page })}`)

export const getStream = (id) => maybeAuthorized(`/api/streams/${id}`)

export const createStream = (payload) =>
    authorized('/api/streams', { method: 'POST', body: payload })

export const updateStream = (id, payload) =>
    authorized(`/api/streams/${id}`, { method: 'PUT', body: payload })

export const deleteStream = (id) => authorized(`/api/streams/${id}`, { method: 'DELETE' })

export const toggleLike = (streamId) =>
    authorized(`/api/streams/${streamId}/like`, { method: 'POST' })

// ---- 댓글 ----

export const getComments = (streamId, page = 0) =>
    maybeAuthorized(`/api/streams/${streamId}/comments${query({ page })}`)

export const createComment = (streamId, content) =>
    authorized(`/api/streams/${streamId}/comments`, { method: 'POST', body: { content } })

export const updateComment = (streamId, commentId, content) =>
    authorized(`/api/streams/${streamId}/comments/${commentId}`, {
        method: 'PUT',
        body: { content },
    })

export const deleteComment = (streamId, commentId) =>
    authorized(`/api/streams/${streamId}/comments/${commentId}`, { method: 'DELETE' })

// ---- 채널 ----

export const getChannel = (channelId) => maybeAuthorized(`/api/channels/${channelId}`)

export const getChannelStreams = (channelId, page = 0) =>
    maybeAuthorized(`/api/channels/${channelId}/streams${query({ page })}`)

export const getChannelLiveHistory = (channelId, page = 0) =>
    maybeAuthorized(`/api/channels/${channelId}/live-history${query({ page })}`)

export const toggleSubscribe = (channelId) =>
    authorized(`/api/channels/${channelId}/subscribe`, { method: 'POST' })

/** 방송 중이 아니면 404 라 null 로 바꿔 돌려준다. */
export const getChannelLive = (channelId) =>
    maybeAuthorized(`/api/channels/${channelId}/live`).catch(() => null)

// ---- 라이브 ----

export const getLives = (page = 0) => maybeAuthorized(`/api/lives${query({ page })}`)

export const getLive = (liveId) => maybeAuthorized(`/api/lives/${liveId}`)

export const getChatHistory = (liveId) => maybeAuthorized(`/api/lives/${liveId}/chats`)

export const getLiveSetting = () => authorized('/api/lives/settings')

export const updateLiveSetting = (payload) =>
    authorized('/api/lives/settings', { method: 'PUT', body: payload })

// ---- 알림 ----

export const getNotifications = (page = 0) => authorized(`/api/notifications${query({ page })}`)

export const getUnreadCount = () => authorized('/api/notifications/unread-count')

export const markNotificationRead = (id) =>
    authorized(`/api/notifications/${id}/read`, { method: 'PATCH' })

export const markAllNotificationsRead = () =>
    authorized('/api/notifications/read-all', { method: 'POST' })

// ---- 신고 · 카테고리 ----

export const createReport = (targetType, targetId, reason) =>
    authorized('/api/reports', { method: 'POST', body: { targetType, targetId, reason } })

/** 카테고리 목록은 공개라 토큰 없이 조회한다. */
export const getCategories = async () => unwrap(await request('/api/categories'))

export const createCategory = (name) =>
    authorized('/api/categories', { method: 'POST', body: { name } })

// ---- 파일 업로드 ----

export async function uploadFile(file) {
    const formData = new FormData()
    formData.append('file', file)

    return authorized('/api/files/upload', { method: 'POST', formData })
}

// ---- 관리자 ----

export const getReports = (status, page = 0) =>
    authorized(`/api/admin/reports${query({ status, page })}`)

export const updateReportStatus = (id, status) =>
    authorized(`/api/admin/reports/${id}`, { method: 'PATCH', body: { status } })

export const getUsers = (role, page = 0) =>
    authorized(`/api/admin/users${query({ role, page })}`)

export const changeUserRole = (id, role) =>
    authorized(`/api/admin/users/${id}/role`, { method: 'PATCH', body: { role } })
