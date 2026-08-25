import { useState } from 'react'
import { login } from '../api.js'

export default function LoginForm({ onLoggedIn }) {
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState(null)
    const [busy, setBusy] = useState(false)

    async function handleSubmit(event) {
        event.preventDefault()

        setBusy(true)
        setError(null)

        try {
            await login(email, password)
            await onLoggedIn()
        } catch (e) {
            setError(e.message)
        } finally {
            setBusy(false)
        }
    }

    return (
        <div className="admin__panel admin__login">
            <h1 className="admin__title">관리자 로그인</h1>

            {error && <p className="admin__error">{error}</p>}

            <form onSubmit={handleSubmit}>
                <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="이메일"
                    autoComplete="username"
                    required
                />

                <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="비밀번호"
                    autoComplete="current-password"
                    required
                />

                <button className="admin__button--primary" type="submit" disabled={busy}>
                    {busy ? '로그인 중…' : '로그인'}
                </button>
            </form>
        </div>
    )
}
