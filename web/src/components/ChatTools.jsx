import { useState } from 'react'
import {
    addBannedWord,
    getBannedWords,
    getChatRestrictions,
    getModerators,
    liftChannelRestriction,
    removeBannedWord,
    removeModerator,
} from '../api.js'
import { formatWhen } from '../time.js'
import { useAsyncData } from '../useAsyncData.js'

/**
 * 채팅 도구(내 채널). 매니저, 금칙어, 채팅이 막힌 사람을 한곳에서 본다.
 * 매니저는 채팅창에서 시청자 이름 옆의 ⋯ 를 눌러 지정한다 — 여기서는 목록을 보고 해제한다.
 */
export default function ChatTools() {
    return (
        <details>
            <summary>채팅 관리</summary>

            <ModeratorList />
            <BannedWords />
            <RestrictionList />
        </details>
    )
}

function ModeratorList() {
    const { data: moderators, error, reload, fail } = useAsyncData(getModerators, [])

    async function handleRemove(moderator) {
        try {
            await removeModerator(moderator.userId)
            reload()
        } catch (e) {
            fail(e)
        }
    }

    return (
        <>
            <h4>매니저</h4>
            <p className="meta">방송 채팅창에서 시청자의 ⋯ 를 눌러 지정할 수 있습니다. 최대 10명입니다.</p>

            {error && <p className="error">{error}</p>}
            {moderators?.length === 0 && <p className="empty">지정한 매니저가 없습니다.</p>}

            <ul>
                {moderators?.map((moderator) => (
                    <li key={moderator.userId}>
                        {moderator.nickname}{' '}
                        <button type="button" onClick={() => handleRemove(moderator)}>
                            해제
                        </button>
                    </li>
                ))}
            </ul>
        </>
    )
}

function BannedWords() {
    const { data: words, error, reload, fail } = useAsyncData(getBannedWords, [])
    const [word, setWord] = useState('')

    async function handleAdd(event) {
        event.preventDefault()

        try {
            await addBannedWord(word)
            setWord('')
            reload()
        } catch (e) {
            fail(e)
        }
    }

    async function handleRemove(item) {
        try {
            await removeBannedWord(item.id)
            reload()
        } catch (e) {
            fail(e)
        }
    }

    return (
        <>
            <h4>금칙어</h4>
            <p className="meta">
                포함된 채팅은 보내지지 않습니다. 대소문자와 띄어쓰기는 구별하지 않습니다. 주인과 매니저에게는 적용되지
                않습니다.
            </p>

            {error && <p className="error">{error}</p>}

            <form className="form form--inline" onSubmit={handleAdd}>
                <input
                    value={word}
                    onChange={(e) => setWord(e.target.value)}
                    maxLength={30}
                    placeholder="막을 단어"
                    required
                />
                <button type="submit">추가</button>
            </form>

            <ul className="chips">
                {words?.map((item) => (
                    <li key={item.id}>
                        {item.word}{' '}
                        <button type="button" aria-label={`${item.word} 삭제`} onClick={() => handleRemove(item)}>
                            ×
                        </button>
                    </li>
                ))}
            </ul>
        </>
    )
}

function RestrictionList() {
    const { data: restrictions, error, reload, fail } = useAsyncData(getChatRestrictions, [])

    async function handleLift(restriction) {
        try {
            await liftChannelRestriction(restriction.userId)
            reload()
        } catch (e) {
            fail(e)
        }
    }

    return (
        <>
            <h4>채팅이 막힌 사람</h4>

            {error && <p className="error">{error}</p>}
            {restrictions?.length === 0 && <p className="empty">막힌 사람이 없습니다.</p>}

            <ul>
                {restrictions?.map((restriction) => (
                    <li key={restriction.userId}>
                        {restriction.nickname}
                        <span className="meta">
                            {' '}
                            · {restriction.permanent ? '강퇴' : `${formatWhen(restriction.restrictedUntil)}까지 정지`}
                            {restriction.reason && ` · ${restriction.reason}`}
                        </span>{' '}
                        <button type="button" onClick={() => handleLift(restriction)}>
                            풀기
                        </button>
                    </li>
                ))}
            </ul>
        </>
    )
}
