import { useState } from 'react'
import { cancelSchedule, createSchedule, getMySchedules, updateSchedule } from '../api.js'
import { AUDIENCE_LABEL, AUDIENCE_OPTIONS } from '../audience.js'
import { formatWhen, fromLocalInput, toLocalInput } from '../time.js'
import { useAsyncData } from '../useAsyncData.js'
import Link from './Link.jsx'

/**
 * 방송 예약 관리. 예약을 올리면 구독자에게 알림이 가고, 시각이 가까워지면 한 번 더 알림이 간다.
 * 예약한 시각 근처에 송출을 시작하면 그 예약이 방송으로 이어져 제목·공개 대상이 그대로 쓰인다.
 */
export default function ScheduleManager() {
    const { data: schedules, error, loading, reload, fail } = useAsyncData(getMySchedules, [])
    const [editing, setEditing] = useState(null)
    const [adding, setAdding] = useState(false)

    async function handleSave(payload) {
        try {
            if (editing) {
                await updateSchedule(editing.id, payload)
            } else {
                await createSchedule(payload)
            }

            setEditing(null)
            setAdding(false)
            reload()
        } catch (e) {
            fail(e)
        }
    }

    async function handleCancel(schedule) {
        if (!window.confirm(`"${schedule.title}" 예약을 취소할까요? 구독자에게 취소 알림이 갑니다.`)) return

        try {
            await cancelSchedule(schedule.id)
            reload()
        } catch (e) {
            fail(e)
        }
    }

    return (
        <details>
            <summary>방송 예약</summary>

            {error && <p className="error">{error}</p>}
            {loading && <p className="empty">불러오는 중…</p>}
            {schedules?.length === 0 && !adding && <p className="empty">예약한 방송이 없습니다.</p>}

            <ul>
                {schedules?.map((schedule) => (
                    <li key={schedule.id}>
                        <Link to={{ view: 'schedule', id: schedule.id }}>{schedule.title}</Link>
                        <span className="meta">
                            {' '}
                            · {formatWhen(schedule.scheduledAt)} · 방송 {AUDIENCE_LABEL[schedule.audience]}
                        </span>{' '}
                        <button
                            type="button"
                            onClick={() => {
                                setEditing(schedule)
                                setAdding(false)
                            }}
                        >
                            고치기
                        </button>{' '}
                        <button type="button" onClick={() => handleCancel(schedule)}>
                            취소
                        </button>
                    </li>
                ))}
            </ul>

            {(adding || editing) && (
                <ScheduleForm
                    key={editing?.id ?? 'new'}
                    initial={editing}
                    onSubmit={handleSave}
                    onClose={() => {
                        setEditing(null)
                        setAdding(false)
                    }}
                />
            )}

            {!adding && !editing && (
                <button type="button" onClick={() => setAdding(true)}>
                    방송 예약하기
                </button>
            )}
        </details>
    )
}

function ScheduleForm({ initial, onSubmit, onClose }) {
    const [title, setTitle] = useState(initial?.title ?? '')
    const [description, setDescription] = useState(initial?.description ?? '')
    const [when, setWhen] = useState(initial ? toLocalInput(initial.scheduledAt) : '')
    const [audience, setAudience] = useState(initial?.audience ?? 'ALL')
    const [chatAudience, setChatAudience] = useState(initial?.chatAudience ?? 'ALL')

    function handleSubmit(event) {
        event.preventDefault()

        onSubmit({
            title,
            description: description || null,
            scheduledAt: fromLocalInput(when),
            audience,
            chatAudience,
        })
    }

    return (
        <form className="form" onSubmit={handleSubmit}>
            <label>
                방송 제목
                <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={100} required />
            </label>

            <label>
                방송 시각
                <input type="datetime-local" value={when} onChange={(e) => setWhen(e.target.value)} required />
            </label>

            <label>
                설명
                <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={2} />
            </label>

            <label>
                방송을 볼 수 있는 사람
                <select value={audience} onChange={(e) => setAudience(e.target.value)}>
                    {AUDIENCE_OPTIONS.map(({ value, label }) => (
                        <option key={value} value={value}>
                            {label}
                        </option>
                    ))}
                </select>
            </label>

            <label>
                채팅할 수 있는 사람
                <select value={chatAudience} onChange={(e) => setChatAudience(e.target.value)}>
                    {AUDIENCE_OPTIONS.map(({ value, label }) => (
                        <option key={value} value={value}>
                            {label}
                        </option>
                    ))}
                </select>
            </label>

            <div>
                <button type="submit">{initial ? '저장' : '예약하기'}</button>{' '}
                <button type="button" onClick={onClose}>
                    닫기
                </button>
            </div>
        </form>
    )
}
