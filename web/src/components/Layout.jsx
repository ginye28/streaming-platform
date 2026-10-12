import { useEffect, useRef, useState } from 'react'
import { getMySubscriptions, getUnreadCount } from '../api.js'
import { useAuth } from '../useAuth.js'
import { loginTo, navigate, useRoute } from '../router.js'
import { setPerfMode, usePerf } from '../perf.js'
import { setTheme, useTheme } from '../theme.js'
import Avatar from '../components/Avatar.jsx'
import Link from '../components/Link.jsx'
import ServerWaking from '../components/ServerWaking.jsx'
import { useAsyncData } from '../useAsyncData.js'

/**
 * 위에는 머리(검색 · 알림 · 계정), 왼쪽에는 허공에 떠 있는 유리 독(메뉴와 구독 채널).
 * 독은 좁게 접혀 있다가 마우스를 올리거나 키보드로 들어오면 펼쳐진다.
 * 좁은 화면에서는 독을 접고 머리의 메뉴만 남긴다.
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
            <ServerWaking />
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
                            placeholder="방송, 채널, 영상을 찾아보세요"
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
                    {me ? (
                        <>
                            <NotificationLink />
                            <AccountMenu me={me} onLogout={logout} />
                        </>
                    ) : (
                        <>
                            <ThemeButton />
                            {view !== 'auth' && (
                                <Link to={loginTo()} className="c-pill c-pill--primary">
                                    로그인
                                </Link>
                            )}
                        </>
                    )}
                </div>
            </header>

            <Dock me={me} view={view} />

            <div className="shell">
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

/* ── 독 ── */

function Icon({ name }) {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
            {name === 'home' && (
                <>
                    <path d="M3 11.5 12 4l9 7.5" />
                    <path d="M5.5 10v10h13V10" />
                </>
            )}
            {name === 'live' && (
                <>
                    <circle cx="12" cy="12" r="2.4" />
                    <path d="M7.2 7.2a6.8 6.8 0 0 0 0 9.6M16.8 7.2a6.8 6.8 0 0 1 0 9.6M4.4 4.4a10.8 10.8 0 0 0 0 15.2M19.6 4.4a10.8 10.8 0 0 1 0 15.2" />
                </>
            )}
            {name === 'heart' && <path d="M12 20s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7.2a4.3 4.3 0 0 1 7.5 2.6C19.5 15.4 12 20 12 20z" />}
            {name === 'upload' && (
                <>
                    <path d="M12 16V5M7.5 9.5 12 5l4.5 4.5" />
                    <path d="M5 19h14" />
                </>
            )}
            {name === 'camera' && (
                <>
                    <rect x="3" y="6.5" width="12" height="11" rx="3" />
                    <path d="m15 11 6-3.5v9L15 13" />
                </>
            )}
            {name === 'chevron' && <path d="m6 9 6 6 6-6" />}
            {name === 'bell' && (
                <>
                    <path d="M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z" />
                    <path d="M10 20.5a2.2 2.2 0 0 0 4 0" />
                </>
            )}
        </svg>
    )
}

function Dock({ me, view }) {
    return (
        <nav className="c-dock" aria-label="주요 메뉴">
            <Link to={{ view: 'home' }} className="c-dock__logo" aria-label="Streaming Platform 홈">
                <span className="c-dock__mark" aria-hidden="true">SP</span>
                <span className="c-dock__label">Streaming Platform</span>
            </Link>

            <DockLink to={{ view: 'home' }} icon="home" active={view === 'home'}>
                홈
            </DockLink>
            <DockLink to={{ view: 'lives' }} icon="live" active={view === 'lives' || view === 'live'}>
                라이브
            </DockLink>
            {me && (
                <>
                    <DockLink to={{ view: 'subscribed' }} icon="heart" active={view === 'subscribed'}>
                        구독
                    </DockLink>
                    <DockLink to={{ view: 'upload' }} icon="upload" active={view === 'upload'}>
                        올리기
                    </DockLink>
                    <DockLink to={{ view: 'broadcast' }} icon="camera" active={view === 'broadcast'}>
                        방송하기
                    </DockLink>
                </>
            )}

            {me && <DockSubscriptions />}

            <span className="c-dock__spacer" />

            {me && (
                <Link to={{ view: 'me' }} className="c-dock__link c-dock__me" aria-label="내 계정" aria-current={view === 'me' ? 'page' : undefined}>
                    <Avatar src={me.profileImage} name={me.nickname} size={32} />
                    <span className="c-dock__label">{me.nickname}</span>
                </Link>
            )}
        </nav>
    )
}

function DockLink({ to, icon, active, children }) {
    return (
        <Link to={to} className={`c-dock__link${active ? ' c-dock__link--on' : ''}`} aria-current={active ? 'page' : undefined}>
            <Icon name={icon} />
            <span className="c-dock__label">{children}</span>
        </Link>
    )
}

/** 구독한 채널. 방송 중인 채널이 위로 오고, 접힌 독에서는 얼굴만 보인다. */
function DockSubscriptions() {
    const { data } = useAsyncData(() => getMySubscriptions(0), [])
    const channels = [...(data?.content ?? [])].sort((a, b) => Number(b.live) - Number(a.live))

    if (channels.length === 0) return null

    return (
        <section className="c-dock__channels" aria-label="구독 채널">
            <h2 className="c-dock__title">구독 채널</h2>

            <ul>
                {channels.map((channel) => (
                    <li key={channel.id}>
                        <Link to={{ view: 'channel', id: channel.id }} className="c-dock__link c-dock__channel">
                            <Avatar src={channel.profileImage} name={channel.nickname} size={30} live={channel.live} />
                            <span className="c-dock__label">{channel.nickname}</span>
                            {channel.live && <span className="c-dock__live c-dock__label">LIVE</span>}
                        </Link>
                    </li>
                ))}
            </ul>
        </section>
    )
}

/* ── 머리의 계정 쪽 ── */

function NotificationLink() {
    const { data } = useAsyncData(getUnreadCount, [])
    const unread = data?.unreadCount ?? 0

    return (
        <Link to={{ view: 'notifications' }} className="c-pill c-pill--icon nav__bell" data-tip="알림" aria-label={unread > 0 ? `알림, 읽지 않은 알림 ${unread}개` : '알림'}>
            <Icon name="bell" />
            {unread > 0 && (
                <span className="nav__badge" aria-hidden="true">
                    {unread}
                </span>
            )}
        </Link>
    )
}

/** 로그아웃 상태에서 쓰는 밝게/어둡게 버튼. 로그인하면 계정 메뉴 안으로 들어간다. */
function ThemeButton() {
    const theme = useTheme()
    const dark = theme === 'dark'

    return (
        <button
            type="button"
            className="c-pill"
            onClick={() => setTheme(dark ? 'light' : 'dark')}
            data-tip={dark ? '밝은 화면으로 바꾸기' : '어두운 화면으로 바꾸기'}
        >
            {dark ? '밝게' : '어둡게'}
        </button>
    )
}

/**
 * 내 얼굴과 이름 아래로 펼쳐지는 메뉴: 내 계정 · (관리자) · 화면 모드 · 로그아웃.
 * 바깥을 누르거나 Esc 로 닫고, 위아래 방향키로 항목을 옮긴다.
 */
function AccountMenu({ me, onLogout }) {
    const [open, setOpen] = useState(false)
    const rootRef = useRef(null)
    const buttonRef = useRef(null)
    const listRef = useRef(null)
    const theme = useTheme()
    const dark = theme === 'dark'
    const perf = usePerf()

    useEffect(() => {
        if (!open) return undefined

        listRef.current?.querySelector('[role="menuitem"]')?.focus()

        const onPointer = (event) => {
            if (!rootRef.current?.contains(event.target)) setOpen(false)
        }
        document.addEventListener('pointerdown', onPointer)

        return () => document.removeEventListener('pointerdown', onPointer)
    }, [open])

    function close(focusButton) {
        setOpen(false)
        if (focusButton) buttonRef.current?.focus()
    }

    function handleKeyDown(event) {
        const items = [...listRef.current.querySelectorAll('[role="menuitem"]')]
        const index = items.indexOf(document.activeElement)

        if (event.key === 'ArrowDown') {
            items[(index + 1) % items.length].focus()
            event.preventDefault()
        } else if (event.key === 'ArrowUp') {
            items[(index - 1 + items.length) % items.length].focus()
            event.preventDefault()
        } else if (event.key === 'Escape') {
            close(true)
            event.preventDefault()
        } else if (event.key === 'Tab') {
            close(false)
        }
    }

    return (
        <div className="c-menu" ref={rootRef}>
            <button
                ref={buttonRef}
                type="button"
                className="c-pill c-pill--me"
                aria-haspopup="menu"
                aria-expanded={open}
                onClick={() => setOpen((value) => !value)}
                data-tip="내 메뉴"
            >
                <Avatar src={me.profileImage} name={me.nickname} size={26} />
                <span className="c-pill__name">{me.nickname}</span>
                <span className="c-pill__chevron">
                    <Icon name="chevron" />
                </span>
            </button>

            {open && (
                <div className="c-menu__list" role="menu" aria-label="내 메뉴" ref={listRef} onKeyDown={handleKeyDown}>
                    <Link to={{ view: 'me' }} role="menuitem" className="c-menu__item" onClick={() => close(false)}>
                        내 계정
                    </Link>
                    {me.role === 'ADMIN' && (
                        <Link to={{ view: 'admin' }} role="menuitem" className="c-menu__item" onClick={() => close(false)}>
                            관리자
                        </Link>
                    )}
                    <button
                        type="button"
                        role="menuitem"
                        className="c-menu__item"
                        onClick={() => {
                            setTheme(dark ? 'light' : 'dark')
                            close(true)
                        }}
                    >
                        <span>화면 모드</span>
                        <span className="c-menu__value">{dark ? '어둡게 → 밝게' : '밝게 → 어둡게'}</span>
                    </button>
                    <button
                        type="button"
                        role="menuitem"
                        className="c-menu__item"
                        onClick={() => {
                            // 자동 → 가볍게 → 전체 → 자동. 느린 기기에서 사용자가 직접 줄이거나 되돌릴 수 있다.
                            setPerfMode({ auto: 'lite', lite: 'full', full: 'auto' }[perf.mode])
                            close(true)
                        }}
                    >
                        <span>화면 효과</span>
                        <span className="c-menu__value">
                            {{ auto: `자동 (${perf.level === 'lite' ? '가볍게' : '전체'})`, lite: '가볍게', full: '전체' }[perf.mode]}
                        </span>
                    </button>
                    <button
                        type="button"
                        role="menuitem"
                        className="c-menu__item c-menu__item--danger"
                        onClick={() => {
                            close(false)
                            onLogout()
                        }}
                    >
                        로그아웃
                    </button>
                </div>
            )}
        </div>
    )
}
