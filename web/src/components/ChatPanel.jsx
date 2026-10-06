import { useEffect, useMemo, useRef, useState } from 'react'
import {
    addModerator,
    deleteChatMessage,
    pinChatMessage,
    restrictChatUser,
    setChatAudience,
    setSlowMode,
    unpinChatMessage,
} from '../api.js'
import { AUDIENCE_LABEL, AUDIENCE_OPTIONS, SLOW_MODE_CHOICES } from '../audience.js'
import { donationTier } from '../donation.js'
import { won } from '../payments.js'
import { useChat } from '../useChat.js'
import DonateBox from './DonateBox.jsx'
import Link from './Link.jsx'
import OshiMark from './OshiMark.jsx'

const ROLE_LABEL = { OWNER: '주인', MANAGER: '매니저' }

/** 1초마다 지금 시각을 갱신한다. 후원 띠가 시간이 지나면 사라지게 하려는 것이다. enabled 가 false 면 멈춘다. */
function useNow(enabled) {
    const [now, setNow] = useState(() => Date.now())

    useEffect(() => {
        if (!enabled) return

        const timer = setInterval(() => setNow(Date.now()), 1000)

        return () => clearInterval(timer)
    }, [enabled])

    return now
}

/**
 * 라이브 채팅창. 채팅·후원·고정 메시지를 보이고, 주인과 매니저에게는 운영 도구를 준다.
 *
 * live 는 서버가 준 방송 정보(myRole · chatLocked · slowModeSeconds · chatAudience · pinnedMessage 포함),
 * history 는 접속 직후 채울 지난 채팅(오래된 것부터, 지운 것은 뺀 것)이다.
 */
export default function ChatPanel({ live, history, me }) {
    const chat = useChat(live.id, {
        messages: history,
        pinned: live.pinnedMessage,
        slowModeSeconds: live.slowModeSeconds,
        chatAudience: live.chatAudience,
    })

    const [content, setContent] = useState('')
    const [localNotice, setLocalNotice] = useState(null)
    const [menuFor, setMenuFor] = useState(null)
    const listRef = useRef(null)

    // 맨 아래를 보고 있을 때만 새 메시지를 따라간다. 위로 올려 읽는 중이면 붙잡아 끌어내리지 않고,
    // 그동안 쌓인 새 메시지 수를 칩으로 알려 준다.
    const [stuck, setStuck] = useState(true)
    const [leftAt, setLeftAt] = useState(0)

    const isOwner = live.myRole === 'OWNER'
    const isStaff = isOwner || live.myRole === 'MANAGER'

    // 방송 중 바뀐 채팅 대상은 내 구독 등급을 다시 읽지 않으면 알 수 없으므로, 처음 받은 잠금 판정은 대상이 그대로일 때만 쓴다.
    const chatLocked = live.chatLocked && chat.chatAudience === live.chatAudience
    const canSend = Boolean(me) && !chatLocked

    const donationStrip = useDonationStrip(chat.messages)

    // 운영 도구·후원에서 난 오류도 몇 초 뒤에 저절로 사라진다(채팅 거절 이유는 useChat 이 같은 식으로 지운다).
    useEffect(() => {
        if (!localNotice) return

        const timer = setTimeout(() => setLocalNotice(null), 5000)

        return () => clearTimeout(timer)
    }, [localNotice])

    // 맨 아래를 보고 있으면 새 메시지가 올 때 맨 아래로 붙인다.
    useEffect(() => {
        const list = listRef.current

        if (list && stuck) {
            list.scrollTop = list.scrollHeight
        }
    }, [chat.messages, stuck])

    function handleScroll(event) {
        const list = event.currentTarget
        const atBottom = list.scrollHeight - list.scrollTop - list.clientHeight < 40

        if (atBottom !== stuck) {
            setStuck(atBottom)

            if (!atBottom) setLeftAt(chat.messages.length)
        }
    }

    const unseen = stuck ? 0 : Math.max(0, chat.messages.length - leftAt)

    const notice = localNotice ?? chat.notice

    async function run(action) {
        setLocalNotice(null)

        try {
            await action()
            setMenuFor(null)
        } catch (e) {
            setLocalNotice(e.message)
        }
    }

    function handleSubmit(event) {
        event.preventDefault()

        if (chat.send(content.trim())) {
            setContent('')
        }
    }

    return (
        <aside className="chat" aria-label="채팅">
            <div className="chat__header">
                채팅
                {chat.viewerCount != null && <span className="meta"> · 시청자 {chat.viewerCount}명</span>}
                {!chat.connected && <span className="meta"> · 연결 중…</span>}
            </div>

            {isStaff && (
                <div className="chat__tools">
                    <label>
                        슬로우 모드{' '}
                        <select
                            value={chat.slowModeSeconds}
                            onChange={(e) => run(() => setSlowMode(live.id, Number(e.target.value)))}
                        >
                            {SLOW_MODE_CHOICES.map((seconds) => (
                                <option key={seconds} value={seconds}>
                                    {seconds === 0 ? '끔' : `${seconds}초`}
                                </option>
                            ))}
                        </select>
                    </label>

                    {isOwner && (
                        <label>
                            채팅{' '}
                            <select
                                value={chat.chatAudience}
                                onChange={(e) => run(() => setChatAudience(live.id, e.target.value))}
                            >
                                {AUDIENCE_OPTIONS.map(({ value, label }) => (
                                    <option key={value} value={value}>
                                        {label}
                                    </option>
                                ))}
                            </select>
                        </label>
                    )}
                </div>
            )}

            {!isStaff && (chat.slowModeSeconds > 0 || chat.chatAudience !== 'ALL') && (
                <p className="chat__banner">
                    {chat.slowModeSeconds > 0 && `슬로우 모드 ${chat.slowModeSeconds}초`}
                    {chat.slowModeSeconds > 0 && chat.chatAudience !== 'ALL' && ' · '}
                    {chat.chatAudience !== 'ALL' && `${AUDIENCE_LABEL[chat.chatAudience]} 채팅`}
                </p>
            )}

            {chat.pinned && (
                <div className="chat__pinned">
                    <span className="chat__pinned-label">고정</span>
                    <span className="chat__pinned-text">
                        <strong>{chat.pinned.nickname}</strong> {chat.pinned.content}
                    </span>
                    {isStaff && (
                        <button type="button" onClick={() => run(() => unpinChatMessage(live.id))}>
                            해제
                        </button>
                    )}
                </div>
            )}

            {donationStrip.length > 0 && (
                <ul className="chat__donations" aria-label="최근 후원">
                    {donationStrip.map((message) => (
                        <li
                            key={message.id}
                            className={`chat-donation chat-donation--t${donationTier(message.donationAmount)}`}
                        >
                            <strong>{message.nickname}</strong>{' '}
                            <span className="chat-amount">{won(message.donationAmount)}</span>
                            {message.content && <span> {message.content}</span>}
                        </li>
                    ))}
                </ul>
            )}

            <div className="chat__body">
            <ul
                className="chat__list"
                ref={listRef}
                role="log"
                aria-live="polite"
                aria-relevant="additions"
                aria-label="채팅 메시지"
                onScroll={handleScroll}
            >
                {chat.messages.map((message) => (
                    <ChatLine
                        key={message.id}
                        message={message}
                        me={me}
                        isOwner={isOwner}
                        isStaff={isStaff}
                        open={menuFor === message.id}
                        onToggle={() => setMenuFor((prev) => (prev === message.id ? null : message.id))}
                        onDelete={() => run(() => deleteChatMessage(live.id, message.id))}
                        onPin={() => run(() => pinChatMessage(live.id, message.id))}
                        onTimeout={() =>
                            run(() => restrictChatUser(live.id, { userId: message.userId, minutes: 10 }))
                        }
                        onBan={() => {
                            if (!window.confirm(`${message.nickname} 님을 이 채널 채팅에서 강퇴할까요? 쓴 메시지도 지워집니다.`)) return

                            run(() => restrictChatUser(live.id, { userId: message.userId, purge: true }))
                        }}
                        onAppoint={() => {
                            if (!window.confirm(`${message.nickname} 님을 매니저로 지정할까요?`)) return

                            run(async () => {
                                await addModerator(message.userId)
                                setLocalNotice(`${message.nickname} 님을 매니저로 지정했습니다.`)
                            })
                        }}
                    />
                ))}
            </ul>

            {unseen > 0 && (
                <button type="button" className="chat__jump" onClick={() => setStuck(true)}>
                    새 메시지 {unseen > 99 ? '99+' : unseen}개{' '}
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" style={{ verticalAlign: '-2px' }}>
                        <path d="M12 5v14M6 13l6 6 6-6" />
                    </svg>
                </button>
            )}
            </div>

            {notice && (
                <p className="chat__notice" role="status">
                    {notice}
                </p>
            )}

            {me && !isOwner && (
                <DonateBox liveId={live.id} onFail={(message) => setLocalNotice(message)} />
            )}

            {!me && (
                <div className="donate">
                    <Link to={{ view: 'auth' }} className="donate__open">
                        후원하려면 로그인
                    </Link>
                </div>
            )}

            {canSend ? (
                <form className="chat__form" onSubmit={handleSubmit}>
                    <input
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                        placeholder="메시지를 입력하세요"
                        maxLength={200}
                        required
                    />
                    <button type="submit" disabled={!chat.connected}>
                        보내기
                    </button>
                </form>
            ) : !me ? (
                <div className="chat__form">
                    <Link to={{ view: 'auth' }} className="cta cta--block">
                        로그인하고 채팅 참여하기
                    </Link>
                </div>
            ) : (
                <p className="meta chat__form">
                    {`${AUDIENCE_LABEL[live.chatAudience]} 채팅할 수 있습니다. 구독하면 참여할 수 있어요.`}
                </p>
            )}
        </aside>
    )
}

/** 지금 채팅창 위에 띠로 남아 있는 후원들. 금액이 큰 순으로 최대 3개. */
function useDonationStrip(messages) {
    const hasDonation = messages.some((m) => m.pinExpiresAt)
    const now = useNow(hasDonation)

    return useMemo(
        () =>
            messages
                .filter((m) => m.pinExpiresAt && m.pinExpiresAt > now)
                .sort((a, b) => b.donationAmount - a.donationAmount)
                .slice(0, 3),
        [messages, now]
    )
}

function ChatLine({ message, me, isOwner, isStaff, open, onToggle, onDelete, onPin, onTimeout, onBan, onAppoint }) {
    const donation = message.donationAmount != null
    const mine = me?.id === message.userId

    // 주인은 누구든, 매니저는 주인과 다른 매니저를 뺀 사람에게 운영 도구를 쓴다.
    const canModerateTarget = isStaff && !mine && message.role !== 'OWNER' && (isOwner || message.role !== 'MANAGER')

    const showMenu = isStaff || mine

    return (
        <li className={donation ? `chat-line chat-line--donation chat-line--t${donationTier(message.donationAmount)}` : 'chat-line'}>
            {donation && <div className="chat-line__amount">{won(message.donationAmount)} 후원</div>}

            <OshiMark url={message.oshiMarkUrl} tier={message.oshiTier} />

            {message.role && <span className={`chat-badge chat-badge--${message.role.toLowerCase()}`}>{ROLE_LABEL[message.role]}</span>}

            <strong>{message.nickname}</strong> {message.content}

            {showMenu && (
                <button type="button" className="chat-line__more" aria-label={`${message.nickname} 메시지 도구`} aria-expanded={open} onClick={onToggle}>
                    ⋯
                </button>
            )}

            {open && (
                <div className="chat-line__menu">
                    <button type="button" onClick={onDelete}>삭제</button>
                    {isStaff && <button type="button" onClick={onPin}>고정</button>}
                    {canModerateTarget && <button type="button" onClick={onTimeout}>10분 정지</button>}
                    {canModerateTarget && <button type="button" onClick={onBan}>강퇴</button>}
                    {isOwner && canModerateTarget && message.role == null && (
                        <button type="button" onClick={onAppoint}>매니저 지정</button>
                    )}
                </div>
            )}
        </li>
    )
}
