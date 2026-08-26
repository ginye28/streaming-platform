import { useCallback, useEffect, useState } from 'react'
import { getAccessToken, getMe, logout as apiLogout } from './api.js'
import { AuthContext } from './useAuth.js'

export function AuthProvider({ children }) {
    const [me, setMe] = useState(null)
    const [loading, setLoading] = useState(true)

    const settle = useCallback((user) => {
        setMe(user)
        setLoading(false)
    }, [])

    // 새로고침해도 저장된 토큰으로 로그인 상태를 되살린다.
    useEffect(() => {
        let cancelled = false

        const fetchMe = getAccessToken() ? getMe() : Promise.resolve(null)

        fetchMe.then(
            (user) => !cancelled && settle(user),
            () => !cancelled && settle(null)
        )

        return () => {
            cancelled = true
        }
    }, [settle])

    const refreshMe = useCallback(
        () => getMe().then(settle, () => settle(null)),
        [settle]
    )

    const logout = useCallback(async () => {
        await apiLogout()
        setMe(null)
    }, [])

    return (
        <AuthContext value={{ me, loading, refreshMe, logout }}>
            {children}
        </AuthContext>
    )
}
