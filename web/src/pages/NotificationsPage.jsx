import { useState } from 'react'
import {
    getNotifications,
    markAllNotificationsRead,
    markNotificationRead,
} from '../api.js'
import Pager from '../components/Pager.jsx'
import Link from '../components/Link.jsx'
import { useAsyncData } from '../useAsyncData.js'

export default function NotificationsPage() {
    const [pageNumber, setPageNumber] = useState(0)

    const {
        data: page,
        error,
        loading,
        reload,
        fail,
    } = useAsyncData(() => getNotifications(pageNumber), [pageNumber])

    async function handleRead(id) {
        try {
            await markNotificationRead(id)
            reload()
        } catch (e) {
            fail(e)
        }
    }

    async function handleReadAll() {
        try {
            await markAllNotificationsRead()
            reload()
        } catch (e) {
            fail(e)
        }
    }

    return (
        <section>
            <div className="toolbar">
                <h2>알림</h2>
                <button onClick={handleReadAll}>모두 읽음</button>
            </div>

            {error && <p className="error">{error}</p>}
            {loading && <p className="empty">불러오는 중…</p>}

            {!loading && page?.content.length === 0 && <p className="empty">알림이 없습니다.</p>}

            <ul className="notifications">
                {page?.content.map((notification) => (
                    <li key={notification.id} className={notification.read ? 'read' : ''}>
                        <NotificationText notification={notification} />

                        <span className="meta"> {formatDateTime(notification.createdAt)}</span>

                        {!notification.read && (
                            <button onClick={() => handleRead(notification.id)}>읽음</button>
                        )}
                    </li>
                ))}
            </ul>

            <Pager page={page} onChange={setPageNumber} />
        </section>
    )
}

/** 눌렀을 때 갈 곳을 아는 알림만 링크로 만든다. 모르는 종류는 글만 보여 준다. */
function NotificationText({ notification }) {
    const target = linkTarget(notification)

    if (!target) return notification.message

    return <Link to={target}>{notification.message}</Link>
}

function linkTarget({ type, targetId, channelId }) {
    // 유료 만료 임박은 대상 방송이 아니라 연장할 채널로 보낸다.
    if (type === 'PAID_EXPIRING') {
        return channelId ? { view: 'channel', id: channelId } : null
    }

    if (!targetId) return null

    switch (type) {
        // 방송 예약 알림은 대기실로 간다.
        case 'LIVE_SCHEDULED':
        case 'LIVE_REMINDER':
            return { view: 'schedule', id: targetId }
        // 후원 알림은 후원이 들어온 방송으로 간다.
        case 'LIVE_START':
        case 'DONATION':
            return { view: 'live', id: targetId }
        case 'STREAM_COMMENT':
        case 'COMMENT_REPLY':
            return { view: 'stream', id: targetId }
        default:
            return null
    }
}

function formatDateTime(value) {
    return value ? value.replace('T', ' ').slice(0, 16) : ''
}
