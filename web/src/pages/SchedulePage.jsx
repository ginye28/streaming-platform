import { useEffect, useState } from 'react'
import { getSchedule } from '../api.js'
import ChannelSubscribe from '../components/ChannelSubscribe.jsx'
import Link from '../components/Link.jsx'
import { navigate } from '../router.js'
import { countdown, formatWhen } from '../time.js'
import { useAsyncData } from '../useAsyncData.js'

const AUDIENCE_NOTE = {
    SUBSCRIBERS: '구독자 전용 방송입니다.',
    PAID: '유료 구독자 전용 방송입니다.',
}

/** 방송이 시작됐는지 다시 묻는 간격(ms). */
const POLL_MS = 10000

/**
 * 방송 대기실. 시작 시각까지 카운트다운을 보여 주고, 방송이 시작되면 저절로 방송 화면으로 넘어간다.
 * 예약이 취소되거나 시각이 한참 지나 내려갔다면 그렇게 알려 준다.
 */
export default function SchedulePage({ id }) {
    const { data: schedule, error, loading, reload } = useAsyncData(() => getSchedule(id), [id])

    const waiting = schedule?.status === 'SCHEDULED'

    const now = useTicker(waiting)

    // 방송이 시작됐는지 주기적으로 확인한다. 대기실을 열어 둔 채 기다리는 사람이 많기 때문이다.
    useEffect(() => {
        if (!waiting) return

        const timer = setInterval(reload, POLL_MS)

        return () => clearInterval(timer)
    }, [waiting, reload])

    // 시작되면 방송으로 넘어간다. 뒤로 가기가 대기실로 돌아오지 않게 현재 기록을 갈아 끼운다.
    useEffect(() => {
        if (schedule?.status === 'STARTED' && schedule.liveId) {
            window.history.replaceState({}, '', `?view=live&id=${schedule.liveId}`)
            navigate({ view: 'live', id: schedule.liveId })
        }
    }, [schedule])

    if (loading) return <p className="empty">불러오는 중…</p>
    if (error) return <p className="error">{error}</p>
    if (!schedule) return null

    const left = countdown(schedule.scheduledAt, now)

    return (
        <section className="waiting-room">
            <h2>{schedule.title}</h2>

            <p className="meta">
                <Link to={{ view: 'channel', id: schedule.channelId }}>{schedule.nickname}</Link>
                {' · '}
                {formatWhen(schedule.scheduledAt)}
            </p>

            {waiting && (
                <div className="waiting-room__clock" role="timer" aria-live="off">
                    {left.done ? (
                        <p>곧 시작합니다. 잠시만 기다려 주세요…</p>
                    ) : (
                        <p>
                            {left.days > 0 && <span>{left.days}일 </span>}
                            <strong>
                                {String(left.hours).padStart(2, '0')}:{String(left.minutes).padStart(2, '0')}:
                                {String(left.seconds).padStart(2, '0')}
                            </strong>{' '}
                            뒤에 시작합니다
                        </p>
                    )}
                </div>
            )}

            {schedule.status === 'CANCELED' && <p className="empty">방송 예약이 취소되었습니다.</p>}
            {schedule.status === 'EXPIRED' && <p className="empty">예정된 시간이 지났지만 방송이 시작되지 않았습니다.</p>}
            {schedule.status === 'STARTED' && !schedule.liveId && <p className="empty">방송이 시작되었습니다.</p>}

            {schedule.description && <p className="description">{schedule.description}</p>}

            {AUDIENCE_NOTE[schedule.audience] && (
                <p className="meta">
                    {AUDIENCE_NOTE[schedule.audience]}
                    {schedule.locked && ' 구독하면 방송을 볼 수 있어요.'}
                </p>
            )}

            <ChannelSubscribe channelId={schedule.channelId} onChanged={reload} />
        </section>
    )
}

/** 대기하는 동안 1초마다 지금 시각을 갱신한다. */
function useTicker(enabled) {
    const [now, setNow] = useState(() => Date.now())

    useEffect(() => {
        if (!enabled) return

        const timer = setInterval(() => setNow(Date.now()), 1000)

        return () => clearInterval(timer)
    }, [enabled])

    return now
}
