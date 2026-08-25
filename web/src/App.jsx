import AdminApp from './admin/AdminApp.jsx'
import { AuthProvider } from './auth.jsx'
import Layout from './components/Layout.jsx'
import AuthPage from './pages/AuthPage.jsx'
import ChannelPage from './pages/ChannelPage.jsx'
import HomePage from './pages/HomePage.jsx'
import LivePage from './pages/LivePage.jsx'
import LivesPage from './pages/LivesPage.jsx'
import MePage from './pages/MePage.jsx'
import NotificationsPage from './pages/NotificationsPage.jsx'
import SearchPage from './pages/SearchPage.jsx'
import StreamPage from './pages/StreamPage.jsx'
import SubscribedPage from './pages/SubscribedPage.jsx'
import UploadPage from './pages/UploadPage.jsx'
import { useRoute } from './router.js'
import './app.css'

function Routes() {
    const { view, id, keyword } = useRoute()

    switch (view) {
        case 'lives':
            return <LivesPage />
        case 'live':
            return <LivePage id={id} />
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
        default:
            return <HomePage />
    }
}

function App() {
    const { view } = useRoute()

    // 관리자 화면은 자체 레이아웃과 로그인 흐름을 가진다.
    if (view === 'admin') {
        return <AdminApp />
    }

    return (
        <AuthProvider>
            <Layout>
                <Routes />
            </Layout>
        </AuthProvider>
    )
}

export default App
