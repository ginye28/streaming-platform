import { useState } from 'react'
import { getAdminLowLatency, getAdminPlayback, setAdminLowLatency } from '../api.js'
import { REASON_LABEL } from '../playbackMode.js'
import { formatDate } from '../time.js'
import { useAsyncData } from '../useAsyncData.js'

/** 숫자 칸마다 "무슨 뜻인지" 한 줄을 붙인다. */
const REASON_HINT = {
    UNSUPPORTED: '이 브라우저가 저지연 재생(MediaSource)을 지원하지 않음 — 일부 모바일·구형 브라우저',
    CODEC: '영상 형식(H.264)을 이 기기가 못 풂',
    BUFFER: '재생 버퍼 오류가 반복돼 포기',
    CONNECTION: '연결이 끊긴 뒤 다시 잇기를 모두 실패했거나 10초 안에 영상이 오지 않음',
    FULL: '저지연 시청자 상한(LOW_LATENCY_MAX_VIEWERS)에 닿아 거절됨',
    FORBIDDEN: '구독자 전용 등으로 저지연 연결이 거절됨',
    UNAVAILABLE: '방송 중계가 아직 열리지 않았거나 이미 닫힘',
    OTHER: '그 밖의 이유(이유를 알리지 않은 보고 포함)',
}

function percent(part, total) {
    return total === 0 ? '—' : `${Math.round((part / total) * 100)}%`
}

/**
 * 저지연 시청 집계. 서버 메모리에만 있어서 서버를 다시 켜면 0 부터다.
 * "저지연으로 보는 비율"은 시청 화면이 알려 온 값이라, 알리기 전에 화면을 닫은 시청자는 빠진다.
 */
export default function PlaybackTab() {
    const { data, error, loading, reload } = useAsyncData(getAdminPlayback, [])
    const { data: lowLatency, error: switchLoadError, reload: reloadSwitch } = useAsyncData(getAdminLowLatency, [])
    const [switching, setSwitching] = useState(false)
    const [switchError, setSwitchError] = useState(null)

    async function toggleLowLatency() {
        setSwitching(true)
        setSwitchError(null)

        try {
            await setAdminLowLatency(!lowLatency.enabled)
            reloadSwitch()
        } catch (e) {
            setSwitchError(e.message)
        } finally {
            setSwitching(false)
        }
    }

    const reported = data ? data.reportedLowLatency + data.reportedHls : 0

    return (
        <div className="admin__panel">
            <div className="admin__toolbar">
                <span className="admin__who">
                    {data?.since ? `${formatDate(data.since)} 부터 센 횟수 · 서버를 다시 켜면 처음부터` : ''}
                </span>
                <button onClick={reload} disabled={loading}>
                    새로고침
                </button>
            </div>

            {lowLatency && (
                <div className="admin__switch">
                    <div>
                        <strong>저지연 중계: {lowLatency.enabled ? '켜짐' : '꺼짐'}</strong>
                        <p>
                            새로 시작하는 방송부터 적용돼요. 이미 방송 중인 건 그대로예요. 서버를 다시 켜면 설정값(INGEST_LOW_LATENCY)으로 돌아가요.
                        </p>
                    </div>
                    <button onClick={toggleLowLatency} disabled={switching}>
                        {lowLatency.enabled ? '끄기' : '켜기'}
                    </button>
                </div>
            )}
            {(switchError || switchLoadError) && <p className="admin__error">{switchError ?? switchLoadError}</p>}

            {error && <p className="admin__error">{error}</p>}
            {loading && !data && <p className="admin__empty">불러오는 중…</p>}

            {data && (
                <>
                    <dl className="admin__stats">
                        <div>
                            <dt>지금 저지연으로 보는 사람</dt>
                            <dd>{data.lowLatencyViewersNow.toLocaleString('ko-KR')}</dd>
                        </div>
                        <div>
                            <dt>저지연으로 보는 비율</dt>
                            <dd>{percent(data.reportedLowLatency, reported)}</dd>
                            <dd className="admin__stat-sub">
                                저지연 {data.reportedLowLatency.toLocaleString('ko-KR')} · 일반 {data.reportedHls.toLocaleString('ko-KR')}
                            </dd>
                        </div>
                        <div>
                            <dt>느려서 끊은 횟수</dt>
                            <dd>{data.slowDisconnects.toLocaleString('ko-KR')}</dd>
                            <dd className="admin__stat-sub">전송 큐가 약 10초 밀린 시청자 (일반 재생으로 이어 봄)</dd>
                        </div>
                        <div>
                            <dt>자리가 없어 거절한 횟수</dt>
                            <dd>{data.fullRejected.toLocaleString('ko-KR')}</dd>
                            <dd className="admin__stat-sub">저지연 연결 {data.lowLatencyJoined.toLocaleString('ko-KR')}회 붙음</dd>
                        </div>
                    </dl>

                    <h3>일반(HLS) 재생으로 넘어간 이유</h3>

                    {data.reportedHls === 0 ? (
                        <p className="admin__empty">아직 일반 재생으로 넘어간 보고가 없습니다.</p>
                    ) : (
                        <div className="admin__scroll">
                            <table>
                                <thead>
                                    <tr>
                                        <th>이유</th>
                                        <th>횟수</th>
                                        <th>뜻</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {Object.keys(REASON_LABEL).map((reason) => (
                                        <tr key={reason}>
                                            <td>{REASON_LABEL[reason]}</td>
                                            <td>{(data.hlsReasons?.[reason] ?? 0).toLocaleString('ko-KR')}</td>
                                            <td>{REASON_HINT[reason]}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}

                    <p className="admin__who">
                        OBS 로 보낸 방송은 처음부터 일반 재생이라 이 숫자에 들어가지 않습니다. 브라우저 방송만 저지연 대상입니다.
                    </p>
                </>
            )}
        </div>
    )
}
