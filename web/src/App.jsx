import { lazy, Suspense } from 'react'
import { AuthProvider } from './auth.jsx'
import ErrorBoundary from './components/ErrorBoundary.jsx'
import Layout from './components/Layout.jsx'
import HomePage from './pages/HomePage.jsx'
import { useRoute } from './router.js'
import './app.css'

/**
 * 첫 화면(홈)만 처음 번들에 넣고 나머지 화면은 열 때 받는다.
 * 재생기(hls.js)와 채팅(stomp)처럼 무거운 것이 홈을 열 때 같이 내려오지 않게 하려는 것이다.
 */
const AdminApp = lazy(() => import('./admin/AdminApp.jsx'))
const AuthPage = lazy(() => import('./pages/AuthPage.jsx'))
const BroadcastPage = lazy(() => import('./pages/BroadcastPage.jsx'))
const ChannelPage = lazy(() => import('./pages/ChannelPage.jsx'))
const LivePage = lazy(() => import('./pages/LivePage.jsx'))
const LivesPage = lazy(() => import('./pages/LivesPage.jsx'))
const MePage = lazy(() => import('./pages/MePage.jsx'))
const NotificationsPage = lazy(() => import('./pages/NotificationsPage.jsx'))
const PayFailPage = lazy(() => import('./pages/PayFailPage.jsx'))
const PayResultPage = lazy(() => import('./pages/PayResultPage.jsx'))
const SchedulePage = lazy(() => import('./pages/SchedulePage.jsx'))
const SearchPage = lazy(() => import('./pages/SearchPage.jsx'))
const StreamPage = lazy(() => import('./pages/StreamPage.jsx'))
const SubscribedPage = lazy(() => import('./pages/SubscribedPage.jsx'))
const UploadPage = lazy(() => import('./pages/UploadPage.jsx'))

function Loading() {
    return (
        <p className="empty" role="status">
            불러오는 중…
        </p>
    )
}

function Routes() {
    const { view, id, keyword } = useRoute()

    switch (view) {
        case 'lives':
            return <LivesPage />
        case 'broadcast':
            return <BroadcastPage />
        case 'live':
            return <LivePage id={id} />
        case 'schedule':
            return <SchedulePage id={id} />
        case 'stream':
            return <StreamPage id={id} />
        case 'channel':
            return <ChannelPage id={id} />
        case 'search':
            return <SearchPage keyword={keyword} />
        case 'subscribed':
            return <SubscribedPage />
        case 'notifications':
            return <NotificationsPage />
        case 'upload':
            return <UploadPage id={id} />
        case 'me':
            return <MePage />
        case 'auth':
            return <AuthPage />
        case 'pay-result':
            return <PayResultPage />
        case 'pay-fail':
            return <PayFailPage />
        default:
            return <HomePage />
    }
}

function App() {
    const { view } = useRoute()

    // 관리자 화면은 자체 레이아웃과 로그인 흐름을 가진다.
    if (view === 'admin') {
        return (
            <Suspense fallback={<Loading />}>
                <AdminApp />
            </Suspense>
        )
    }

    return (
        <AuthProvider>
            <Layout>
                <ErrorBoundary>
                    <Suspense fallback={<Loading />}>
                        <Routes />
                    </Suspense>
                </ErrorBoundary>
            </Layout>
        </AuthProvider>
    )
}

export default App
