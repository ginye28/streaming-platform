import { useCallback, useEffect, useState } from 'react'
import { getAccessToken, getMe, logout } from '../api.js'
import CategoriesTab from './CategoriesTab.jsx'
import LoginForm from './LoginForm.jsx'
import ReportsTab from './ReportsTab.jsx'
import UsersTab from './UsersTab.jsx'
import ThemeToggle from '../components/ThemeToggle.jsx'
import './admin.css'

const TABS = [
    { key: 'reports', label: '신고' },
    { key: 'categories', label: '카테고리' },
    { key: 'users', label: '사용자' },
]

export default function AdminApp() {
    const [me, setMe] = useState(null)
    const [checking, setChecking] = useState(true)
    const [tab, setTab] = useState('reports')

    // 토큰이 만료됐거나 유효하지 않으면 me 는 null 이 되고 로그인 화면으로 돌아간다.
    const fetchMe = () => (getAccessToken() ? getMe() : Promise.resolve(null))

    const settle = useCallback((user) => {
        setMe(user)
        setChecking(false)
    }, [])

    useEffect(() => {
        let cancelled = false

        fetchMe().then(
            (user) => !cancelled && settle(user),
            () => !cancelled && settle(null)
        )

        return () => {
            cancelled = true
        }
    }, [settle])

    /** 로그인 직후 다시 내 정보를 읽어 관리자인지 확인한다. */
    const loadMe = useCallback(
        () => fetchMe().then(settle, () => settle(null)),
        [settle]
    )

    async function handleLogout() {
        await logout()
        setMe(null)
    }

    if (checking) {
        return (
            <div className="admin">
                <p className="admin__empty">확인 중…</p>
            </div>
        )
    }

    if (!me) {
        return (
            <div className="admin">
                <LoginForm onLoggedIn={loadMe} />
            </div>
        )
    }

    if (me.role !== 'ADMIN') {
        return (
            <div className="admin">
                <div className="admin__panel admin__login">
                    <h1 className="admin__title">접근 권한이 없습니다</h1>

                    <p className="admin__who">
                        {me.nickname} 님은 관리자가 아닙니다. 최초 관리자는 서버 설정
                        <code> app.admin.emails </code>
                        로만 지정할 수 있습니다. README 의 &quot;관리자 계정 만들기&quot; 를 참고하세요.
                    </p>

                    <button onClick={handleLogout}>로그아웃</button>
                </div>
            </div>
        )
    }

    return (
        <div className="admin">
            <header className="admin__header">
                <h1 className="admin__title">관리자</h1>

                <div className="admin__toolbar" style={{ margin: 0 }}>
                    <span className="admin__who">{me.nickname} 님</span>
                    <ThemeToggle />
                    <button onClick={handleLogout}>로그아웃</button>
                </div>
            </header>

            <nav className="admin__tabs">
                {TABS.map(({ key, label }) => (
                    <button
                        key={key}
                        className={`admin__tab ${tab === key ? 'admin__tab--active' : ''}`}
                        onClick={() => setTab(key)}
                    >
                        {label}
                    </button>
                ))}
            </nav>

            {tab === 'reports' && <ReportsTab />}
            {tab === 'categories' && <CategoriesTab />}
            {tab === 'users' && <UsersTab myId={me.id} />}
        </div>
    )
}
