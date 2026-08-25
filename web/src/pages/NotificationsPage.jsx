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
                        {notification.type === 'LIVE_START' && notification.targetId ? (
                            <Link to={{ view: 'live', id: notification.targetId }}>
                                {notification.message}
                            </Link>
                        ) : (
                            notification.message
                        )}

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

function formatDateTime(value) {
    return value ? value.replace('T', ' ').slice(0, 16) : ''
}
