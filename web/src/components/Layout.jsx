import { useEffect, useRef, useState } from 'react'
import { getUnreadCount } from '../api.js'
import { useAuth } from '../useAuth.js'
import { loginTo, navigate, useRoute } from '../router.js'
import { setPerfMode, usePerf } from '../perf.js'
import { setTheme, useTheme } from '../theme.js'
import { useAsyncData } from '../useAsyncData.js'
import Avatar from './Avatar.jsx'
import Icon from './Icon.jsx'
import Link from './Link.jsx'
import ServerWaking from './ServerWaking.jsx'

/** 목록이 주인공인 화면은 본문을 넓게 쓴다. */
const WIDE_VIEWS = new Set(['home', 'lives', 'subscribed', 'search'])

/**
 * 맨 위 한 줄(상단바)과 본문. 왼쪽 사이드바는 없다.
 *  - 넓은 화면: 로고 · 홈/라이브/구독 · 가운데 검색 · 방송하기 · 올리기 · 알림 · 계정
 *  - 좁은 화면: 로고 · 검색 아이콘 · 알림 · 계정, 그 아래 한 줄짜리 탭. 영상을 보는 화면에서는 탭도 접는다.
 */
export default function Layout({ children }) {
    const { me, logout } = useAuth()
    const { view, keyword } = useRoute()
    // 좁은 화면에서 검색 아이콘을 누르면 상단바가 검색창으로 바뀐다.
    const [searching, setSearching] = useState(false)
    const toggleRef = useRef(null)

    return (
        <div className="app">
            <ServerWaking />
            <a href="#main" className="skip-link" onClick={skipToMain}>
                본문으로 건너뛰기
            </a>

            <header className={`topbar${searching ? ' topbar--searching' : ''}`}>
                <div className="topbar__row">
                    <Link to={{ view: 'home' }} className="brand" aria-label="스트리밍 플랫폼 홈">
                        <BrandMark />
                        <span className="brand__name">스트리밍 플랫폼</span>
                    </Link>

                    <PrimaryNav me={me} view={view} className="topbar__nav" label="주요 메뉴" />

                    <SearchForm
                        key={view === 'search' ? keyword : ''}
                        initial={view === 'search' ? (keyword ?? '') : ''}
                        searching={searching}
                        onClose={() => {
                            setSearching(false)
                            // 검색창이 닫히면 초점이 사라지지 않게 검색 아이콘으로 돌려준다.
                            toggleRef.current?.focus()
                        }}
                    />

                    <div className="topbar__actions">
                        <button
                            type="button"
                            ref={toggleRef}
                            className="icon-btn topbar__search-toggle"
                            aria-label="검색"
                            onClick={() => setSearching(true)}
                        >
                            <Icon name="search" />
                        </button>

                        {me ? (
                            <>
                                <Link to={{ view: 'upload' }} className="icon-btn topbar__upload" aria-label="영상 올리기" title="영상 올리기">
                                    <Icon name="upload" />
                                </Link>
                                <Link to={{ view: 'broadcast' }} className="btn btn--tonal topbar__broadcast">
                                    <Icon name="live" size={20} />
                                    방송하기
                                </Link>
                                <NotificationLink />
                                <AccountMenu me={me} onLogout={logout} />
                            </>
                        ) : (
                            <>
                                <ThemeButton />
                                {view !== 'auth' && (
                                    <Link to={loginTo()} className="btn btn--outline">
                                        <Icon name="user" size={20} />
                                        로그인
                                    </Link>
                                )}
                            </>
                        )}
                    </div>
                </div>

                <PrimaryNav me={me} view={view} className="topbar__tabs" label="화면 이동" withBroadcast />
            </header>

            <main className={`main${WIDE_VIEWS.has(view) ? ' main--wide' : ''}`} id="main" tabIndex={-1}>
                {children}
            </main>
        </div>
    )
}

/** 주소에 #main 을 남기지 않고 본문으로 초점만 옮긴다. */
function skipToMain(event) {
    event.preventDefault()
    document.getElementById('main')?.focus()
}

/** 로고: 파란 둥근 사각형 안의 재생 삼각형. */
export function BrandMark() {
    return (
        <svg className="brand__mark" width="32" height="32" viewBox="0 0 32 32" aria-hidden="true">
            <rect width="32" height="32" rx="9" />
            <path d="M12.3 9.7v12.6a.9.9 0 0 0 1.37.77l10.3-6.3a.9.9 0 0 0 0-1.54l-10.3-6.3a.9.9 0 0 0-1.37.77z" />
        </svg>
    )
}

/* ── 주요 메뉴 ── */

function PrimaryNav({ me, view, className, label, withBroadcast = false }) {
    const items = [
        { to: { view: 'home' }, label: '홈', on: view === 'home' },
        { to: { view: 'lives' }, label: '라이브', on: view === 'lives' || view === 'live' },
    ]

    if (me) {
        items.push({ to: { view: 'subscribed' }, label: '구독', on: view === 'subscribed' })
    }

    // 좁은 화면의 탭에는 방송하기도 둔다(넓은 화면에서는 상단바 오른쪽 버튼이 맡는다).
    if (withBroadcast && me) {
        items.push({ to: { view: 'broadcast' }, label: '방송하기', on: view === 'broadcast' })
    }

    return (
        <nav className={className} aria-label={label}>
            {items.map((item) => (
                <Link key={item.label} to={item.to} className="topnav__link" aria-current={item.on ? 'page' : undefined}>
                    {item.label}
                </Link>
            ))}
        </nav>
    )
}

/* ── 검색 ── */

function SearchForm({ initial, searching, onClose }) {
    const [text, setText] = useState(initial)
    const inputRef = useRef(null)

    // 좁은 화면에서 검색창이 열리면 바로 입력할 수 있게 초점을 준다.
    useEffect(() => {
        if (searching) inputRef.current?.focus()
    }, [searching])

    function submit(event) {
        event.preventDefault()

        if (text.trim()) {
            navigate({ view: 'search', keyword: text.trim() })
            onClose()
        }
    }

    return (
        <form className="search" onSubmit={submit} role="search" onKeyDown={(event) => event.key === 'Escape' && onClose()}>
            <button type="button" className="icon-btn search__back" aria-label="검색 닫기" onClick={onClose}>
                <Icon name="back" />
            </button>
            <label className="search__field">
                <span className="visually-hidden">방송, 채널, 영상 검색</span>
                <input
                    ref={inputRef}
                    type="search"
                    value={text}
                    onChange={(event) => setText(event.target.value)}
                    placeholder="방송, 채널, 영상을 찾아보세요"
                    enterKeyHint="search"
                />
            </label>
            <button type="submit" className="search__submit" aria-label="검색">
                <Icon name="search" size={20} />
            </button>
        </form>
    )
}

/* ── 계정 쪽 ── */

function NotificationLink() {
    const { data } = useAsyncData(getUnreadCount, [])
    const unread = data?.unreadCount ?? 0

    return (
        <Link
            to={{ view: 'notifications' }}
            className="icon-btn"
            title="알림"
            aria-label={unread > 0 ? `알림, 읽지 않은 알림 ${unread}개` : '알림'}
        >
            <Icon name="bell" />
            {unread > 0 && (
                <span className="icon-btn__badge" aria-hidden="true">
                    {unread > 99 ? '99+' : unread}
                </span>
            )}
        </Link>
    )
}

/** 로그아웃 상태의 밝게/어둡게 버튼. 로그인하면 계정 메뉴 안으로 들어간다. */
function ThemeButton() {
    const theme = useTheme()
    const dark = theme === 'dark'

    return (
        <button
            type="button"
            className="icon-btn"
            onClick={() => setTheme(dark ? 'light' : 'dark')}
            title={dark ? '밝은 화면으로' : '어두운 화면으로'}
            aria-label={dark ? '밝은 화면으로 바꾸기' : '어두운 화면으로 바꾸기'}
        >
            <Icon name={dark ? 'sun' : 'moon'} />
        </button>
    )
}

const PERF_NEXT = { auto: 'lite', lite: 'full', full: 'auto' }

/**
 * 계정 메뉴: 내 계정 · 영상 올리기 · (관리자) · 화면 모드 · 화면 효과 · 로그아웃.
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

    const perfLabel = {
        auto: `자동 (${perf.level === 'lite' ? '가볍게' : '전체'})`,
        lite: '가볍게',
        full: '전체',
    }[perf.mode]

    return (
        <div className="menu" ref={rootRef}>
            <button
                ref={buttonRef}
                type="button"
                className="menu__trigger"
                aria-haspopup="menu"
                aria-expanded={open}
                aria-label={`${me.nickname} 계정 메뉴`}
                onClick={() => setOpen((value) => !value)}
            >
                <Avatar src={me.profileImage} name={me.nickname} size={32} seed={me.id} />
            </button>

            {open && (
                <div className="menu__list" role="menu" aria-label="계정 메뉴" ref={listRef} onKeyDown={handleKeyDown}>
                    <div className="menu__who">
                        <Avatar src={me.profileImage} name={me.nickname} size={40} seed={me.id} />
                        <div>
                            <strong>{me.nickname}</strong>
                            {me.email && <span>{me.email}</span>}
                        </div>
                    </div>

                    <Link to={{ view: 'me' }} role="menuitem" className="menu__item" onClick={() => close(false)}>
                        <Icon name="user" size={20} />
                        내 계정
                    </Link>
                    <Link to={{ view: 'upload' }} role="menuitem" className="menu__item" onClick={() => close(false)}>
                        <Icon name="upload" size={20} />
                        영상 올리기
                    </Link>
                    {me.role === 'ADMIN' && (
                        <Link to={{ view: 'admin' }} role="menuitem" className="menu__item" onClick={() => close(false)}>
                            <Icon name="shield" size={20} />
                            관리자
                        </Link>
                    )}

                    <div className="menu__rule" role="separator" />

                    <button
                        type="button"
                        role="menuitem"
                        className="menu__item"
                        onClick={() => {
                            setTheme(dark ? 'light' : 'dark')
                            close(true)
                        }}
                    >
                        <Icon name={dark ? 'sun' : 'moon'} size={20} />
                        <span>화면 모드</span>
                        <span className="menu__value">{dark ? '어둡게' : '밝게'}</span>
                    </button>
                    <button
                        type="button"
                        role="menuitem"
                        className="menu__item"
                        onClick={() => {
                            // 자동 → 가볍게 → 전체 → 자동. 느린 기기에서 직접 줄이거나 되돌릴 수 있다.
                            setPerfMode(PERF_NEXT[perf.mode])
                            close(true)
                        }}
                    >
                        <Icon name="gauge" size={20} />
                        <span>화면 효과</span>
                        <span className="menu__value">{perfLabel}</span>
                    </button>

                    <div className="menu__rule" role="separator" />

                    <button
                        type="button"
                        role="menuitem"
                        className="menu__item"
                        onClick={() => {
                            close(false)
                            onLogout()
                        }}
                    >
                        <Icon name="logout" size={20} />
                        로그아웃
                    </button>
                </div>
            )}
        </div>
    )
}
