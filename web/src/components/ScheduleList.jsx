import { assetUrl } from '../assets.js'
import { formatWhen } from '../time.js'
import Link from './Link.jsx'

const AUDIENCE_NOTE = {
    SUBSCRIBERS: '구독자 전용',
    PAID: '유료 구독자 전용',
}

/** 방송 예약 목록. 시각이 가까운 순으로 오며, 누르면 대기실로 간다. */
export default function ScheduleList({ schedules, empty = '예정된 방송이 없습니다.', showChannel = true }) {
    if (!schedules || schedules.length === 0) {
        return <p className="empty">{empty}</p>
    }

    return (
        <ul className="schedule-list">
            {schedules.map((schedule) => (
                <li key={schedule.id} className="schedule-list__item">
                    <Link to={{ view: 'schedule', id: schedule.id }} className="schedule-list__thumb">
                        {schedule.thumbnailUrl ? (
                            <img src={assetUrl(schedule.thumbnailUrl)} alt="" />
                        ) : (
                            <span className="schedule-list__blank">예정</span>
                        )}
                    </Link>

                    <div className="schedule-list__body">
                        <Link to={{ view: 'schedule', id: schedule.id }} className="schedule-list__title">
                            {schedule.title}
                        </Link>

                        <p className="meta">
                            {formatWhen(schedule.scheduledAt)}
                            {showChannel && ` · ${schedule.nickname}`}
                            {AUDIENCE_NOTE[schedule.audience] && ` · ${AUDIENCE_NOTE[schedule.audience]}`}
                        </p>
                    </div>
                </li>
            ))}
        </ul>
    )
}
