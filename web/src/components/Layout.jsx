import { useState } from 'react'
import { getUnreadCount } from '../api.js'
import { useAuth } from '../useAuth.js'
import { navigate } from '../router.js'
import Link from '../components/Link.jsx'
import { useAsyncData } from '../useAsyncData.js'

export default function Layout({ children }) {
    const { me, logout } = useAuth()
    const [keyword, setKeyword] = useState('')

    function handleSearch(event) {
        event.preventDefault()

        if (keyword.trim()) {
            navigate({ view: 'search', keyword: keyword.trim() })
        }
    }

    return (
        <div className="app">
            <header className="nav">
                <Link to={{ view: 'home' }} className="nav__brand">
                    Streaming Platform
                </Link>

                <nav className="nav__links">
                    <Link to={{ view: 'home' }}>홈</Link>
                    <Link to={{ view: 'lives' }}>라이브</Link>
                    {me && <Link to={{ view: 'subscribed' }}>구독</Link>}
                </nav>

                <form className="nav__search" onSubmit={handleSearch}>
                    <input
                        value={keyword}
                        onChange={(e) => setKeyword(e.target.value)}
                        placeholder="영상 검색"
                    />
                    <button type="submit">검색</button>
                </form>

                <div className="nav__account">
                    {me ? (
                        <>
                            <Link to={{ view: 'upload' }}>올리기</Link>
                            <NotificationLink />
                            <Link to={{ view: 'me' }}>{me.nickname}</Link>
                            {me.role === 'ADMIN' && <Link to={{ view: 'admin' }}>관리자</Link>}
                            <button onClick={logout}>로그아웃</button>
                        </>
                    ) : (
                        <Link to={{ view: 'auth' }}>로그인</Link>
                    )}
                </div>
            </header>

            <main className="main">{children}</main>
        </div>
    )
}

function NotificationLink() {
    const { data } = useAsyncData(getUnreadCount, [])
    const unread = data?.unreadCount ?? 0

    return (
        <Link to={{ view: 'notifications' }}>알림{unread > 0 && ` (${unread})`}</Link>
    )
}
