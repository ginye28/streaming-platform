import { useState } from 'react'
import { getMySubscriptions, getUnreadCount } from '../api.js'
import { assetUrl } from '../assets.js'
import { useAuth } from '../useAuth.js'
import { navigate, useRoute } from '../router.js'
import Link from '../components/Link.jsx'
import ThemeToggle from '../components/ThemeToggle.jsx'
import { useAsyncData } from '../useAsyncData.js'

/**
 * 위에는 머리(로고 · 검색 · 계정), 왼쪽에는 메뉴와 구독 채널이 붙는 사이드바.
 * 좁은 화면에서는 사이드바를 접고 머리의 메뉴만 남긴다.
 */
export default function Layout({ children }) {
    const { me, logout } = useAuth()
    const { view } = useRoute()
    const [keyword, setKeyword] = useState('')

    function handleSearch(event) {
        event.preventDefault()

        if (keyword.trim()) {
            navigate({ view: 'search', keyword: keyword.trim() })
        }
    }

    return (
        <div className="app">
            <a href="#main" className="skip-link" onClick={skipToMain}>
                본문으로 건너뛰기
            </a>

            <header className="nav">
                <Link to={{ view: 'home' }} className="nav__brand" aria-label="Streaming Platform 홈">
                    <span className="nav__mark" aria-hidden="true">SP</span>
                    <span className="nav__name">Streaming Platform</span>
                </Link>

                <form className="nav__search" onSubmit={handleSearch} role="search">
                    <label className="nav__search-box">
                        <span className="visually-hidden">영상 검색</span>
                        <input
                            type="search"
                            value={keyword}
                            onChange={(e) => setKeyword(e.target.value)}
                            placeholder="영상 검색"
                        />
                    </label>
                    <button type="submit">검색</button>
                </form>

                <nav className="nav__links" aria-label="메뉴">
                    <Link to={{ view: 'home' }}>홈</Link>
                    <Link to={{ view: 'lives' }}>라이브</Link>
                    {me && <Link to={{ view: 'subscribed' }}>구독</Link>}
                    {me && <Link to={{ view: 'broadcast' }}>방송하기</Link>}
                </nav>

                <div className="nav__account">
                    <ThemeToggle className="nav__theme" />

                    {me ? (
                        <>
                            <NotificationLink />
                            <Link to={{ view: 'me' }} className="nav__me">
                                {me.nickname}
                            </Link>
                            {me.role === 'ADMIN' && <Link to={{ view: 'admin' }}>관리자</Link>}
                            <button onClick={logout}>로그아웃</button>
                        </>
                    ) : (
                        <Link to={{ view: 'auth' }}>로그인</Link>
                    )}
                </div>
            </header>

            <div className="shell">
                <aside className="side" aria-label="사이드 메뉴">
                    <nav className="side__nav" aria-label="주요 메뉴">
                        <SideLink to={{ view: 'home' }} active={view === 'home'}>
                            홈
                        </SideLink>
                        <SideLink to={{ view: 'lives' }} active={view === 'lives' || view === 'live'}>
                            라이브
                        </SideLink>
                        {me && (
                            <>
                                <SideLink to={{ view: 'subscribed' }} active={view === 'subscribed'}>
                                    구독
                                </SideLink>
                                <SideLink to={{ view: 'upload' }} active={view === 'upload'}>
                                    올리기
                                </SideLink>
                            </>
                        )}
                    </nav>

                    {me && <SideSubscriptions />}
                </aside>

                <main className="main" id="main" tabIndex={-1}>
                    {children}
                </main>
            </div>
        </div>
    )
}

/** 주소에 #main 을 남기지 않고 본문으로 초점만 옮긴다. */
function skipToMain(event) {
    event.preventDefault()
    document.getElementById('main')?.focus()
}

function SideLink({ to, active, children }) {
    return (
        <Link
            to={to}
            className={`side__link${active ? ' side__link--on' : ''}`}
            aria-current={active ? 'page' : undefined}
        >
            {children}
        </Link>
    )
}

/** 구독한 채널. 방송 중인 채널이 위로 온다. */
function SideSubscriptions() {
    const { data } = useAsyncData(() => getMySubscriptions(0), [])
    const channels = [...(data?.content ?? [])].sort((a, b) => Number(b.live) - Number(a.live))

    if (channels.length === 0) return null

    return (
        <section className="side__section">
            <h2 className="side__title">구독 채널</h2>

            <ul className="side__channels">
                {channels.map((channel) => (
                    <li key={channel.id}>
                        <Link to={{ view: 'channel', id: channel.id }} className="side__channel">
                            <span className={`side__face${channel.live ? ' side__face--live' : ''}`}>
                                {channel.profileImage ? (
                                    <img src={assetUrl(channel.profileImage)} alt="" loading="lazy" decoding="async" />
                                ) : (
                                    channel.nickname.slice(0, 1)
                                )}
                            </span>
                            <span className="side__channel-name">{channel.nickname}</span>
                            {channel.live && <span className="side__live">LIVE</span>}
                        </Link>
                    </li>
                ))}
            </ul>
        </section>
    )
}

function NotificationLink() {
    const { data } = useAsyncData(getUnreadCount, [])
    const unread = data?.unreadCount ?? 0

    return (
        <Link to={{ view: 'notifications' }} className="nav__bell">
            알림
            {unread > 0 && (
                <span className="nav__badge" aria-label={`읽지 않은 알림 ${unread}개`}>
                    {unread}
                </span>
            )}
        </Link>
    )
}
