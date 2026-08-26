import { useState } from 'react'
import { login, signup } from '../api.js'
import { useAuth } from '../useAuth.js'
import { navigate } from '../router.js'

export default function AuthPage() {
    const { refreshMe } = useAuth()
    const [mode, setMode] = useState('login')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [nickname, setNickname] = useState('')
    const [error, setError] = useState(null)
    const [busy, setBusy] = useState(false)

    async function handleSubmit(event) {
        event.preventDefault()

        setBusy(true)
        setError(null)

        try {
            if (mode === 'signup') {
                await signup(email, password, nickname)
            }

            await login(email, password)
            await refreshMe()
            navigate({ view: 'home' })
        } catch (e) {
            setError(e.message)
        } finally {
            setBusy(false)
        }
    }

    return (
        <section className="narrow">
            <div className="toolbar">
                <button
                    onClick={() => setMode('login')}
                    disabled={mode === 'login'}
                >
                    로그인
                </button>
                <button
                    onClick={() => setMode('signup')}
                    disabled={mode === 'signup'}
                >
                    회원가입
                </button>
            </div>

            {error && <p className="error">{error}</p>}

            <form className="form" onSubmit={handleSubmit}>
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
                    placeholder="비밀번호 (8자 이상)"
                    autoComplete={mode === 'signup' ? 'new-password' : 'current-password'}
                    minLength={8}
                    required
                />

                {mode === 'signup' && (
                    <input
                        value={nickname}
                        onChange={(e) => setNickname(e.target.value)}
                        placeholder="닉네임 (2~20자)"
                        minLength={2}
                        maxLength={20}
                        required
                    />
                )}

                <button type="submit" disabled={busy}>
                    {busy ? '처리 중…' : mode === 'signup' ? '가입하고 로그인' : '로그인'}
                </button>
            </form>
        </section>
    )
}
