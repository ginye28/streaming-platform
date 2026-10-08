import { useState } from 'react'
import { login, signup } from '../api.js'
import { useAuth } from '../useAuth.js'
import { navigate, parseNext, useRoute } from '../router.js'
import { usePageTitle } from '../usePageTitle.js'
import { useSlow } from '../useSlow.js'

/** 어디서 왔는지에 따라, 로그인하면 무엇을 할 수 있는지 말해 준다. 돌아가서 이어서 할 수 있다는 사실만 말한다. */
const CONTEXT = {
    live: '이 방송 채팅에 참여하고 채널을 구독할 수 있어요. 돌아가서 이어서 할 수 있어요.',
    stream: '좋아요와 댓글을 남기고 채널을 구독할 수 있어요. 돌아가서 이어서 할 수 있어요.',
    channel: '이 채널을 구독할 수 있어요. 돌아가서 이어서 할 수 있어요.',
    broadcast: '브라우저에서 바로 방송을 켤 수 있어요. 돌아가서 이어서 할 수 있어요.',
    upload: '영상을 올릴 수 있어요. 돌아가서 이어서 할 수 있어요.',
}

export default function AuthPage() {
    const { refreshMe } = useAuth()
    const { next } = useRoute()
    const back = parseNext(next)
    const [mode, setMode] = useState('login')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [nickname, setNickname] = useState('')
    const [showPassword, setShowPassword] = useState(false)
    const [error, setError] = useState(null)
    const [busy, setBusy] = useState(false)

    const signingUp = mode === 'signup'

    usePageTitle(signingUp ? '회원가입' : '로그인')

    // 무료 서버가 잠들어 있으면 로그인이 첫 요청이 되어 오래 걸린다. 그 이유를 알려 준다.
    const slow = useSlow(busy)

    function switchMode() {
        setMode(signingUp ? 'login' : 'signup')
        setError(null)
    }

    /** 서버가 준 메시지는 그대로, 네트워크가 안 닿으면(브라우저 영어 메시지 대신) 한국어로. */
    function messageOf(e) {
        return e instanceof TypeError ? '서버에 닿지 못했어요. 잠시 뒤에 다시 시도해 주세요.' : e.message
    }

    async function handleSubmit(event) {
        event.preventDefault()

        setBusy(true)
        setError(null)

        try {
            if (signingUp) {
                await signup(email, password, nickname)
            }
        } catch (e) {
            setError(messageOf(e))
            setBusy(false)
            return
        }

        try {
            await login(email, password)
            await refreshMe()
            navigate(back ?? { view: 'home' })
        } catch (e) {
            if (signingUp) {
                // 가입은 됐는데 이어지는 로그인이 실패한 경우다. 가입 모드에 갇히지 않게 로그인으로 돌려 보낸다.
                setMode('login')
                setError(`가입은 됐어요. 로그인해 주세요. ${messageOf(e)}`)
            } else {
                setError(messageOf(e))
            }
        } finally {
            setBusy(false)
        }
    }

    const context = back ? (CONTEXT[back.view] ?? '보던 화면으로 돌아와요.') : null

    return (
        <section className="auth">
            <div className="auth__card">
                <span className="nav__mark" aria-hidden="true">
                    SP
                </span>

                <h1>{signingUp ? '회원가입' : '로그인'}</h1>

                {context && (
                    <p className="auth__context">
                        {signingUp ? '가입하면' : '로그인하면'} {context}
                    </p>
                )}

                <form className="form" onSubmit={handleSubmit}>
                    <label>
                        이메일
                        <input
                            type="email"
                            name="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            autoComplete="username"
                            inputMode="email"
                            autoCapitalize="none"
                            required
                        />
                    </label>

                    <label>
                        <span>
                            비밀번호 <span className="meta">8자 이상</span>
                        </span>
                        <span className="auth__password">
                            <input
                                type={showPassword ? 'text' : 'password'}
                                name="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                autoComplete={signingUp ? 'new-password' : 'current-password'}
                                minLength={8}
                                required
                            />
                            <button
                                type="button"
                                aria-pressed={showPassword}
                                aria-label="비밀번호 표시"
                                onClick={() => setShowPassword((value) => !value)}
                            >
                                {showPassword ? '숨기기' : '보기'}
                            </button>
                        </span>
                    </label>

                    {signingUp && (
                        <label>
                            <span>
                                닉네임 <span className="meta">2~20자</span>
                            </span>
                            <input
                                name="nickname"
                                value={nickname}
                                onChange={(e) => setNickname(e.target.value)}
                                autoComplete="nickname"
                                autoCapitalize="none"
                                minLength={2}
                                maxLength={20}
                                required
                            />
                        </label>
                    )}

                    {error && (
                        <p className="error" role="alert">
                            {error}
                        </p>
                    )}

                    <button type="submit" disabled={busy}>
                        {busy ? '처리 중…' : signingUp ? '가입하고 로그인' : '로그인'}
                    </button>

                    {busy && slow && (
                        <p className="meta" role="status">
                            서버를 깨우는 중이에요. 최대 1분쯤 걸려요. 잠시만 기다려 주세요.
                        </p>
                    )}
                </form>

                <p className="auth__switch">
                    {signingUp ? '이미 계정이 있나요?' : '아직 계정이 없나요?'}{' '}
                    <button type="button" className="empty__link" onClick={switchMode}>
                        {signingUp ? '로그인' : '회원가입'}
                    </button>
                </p>
            </div>
        </section>
    )
}
