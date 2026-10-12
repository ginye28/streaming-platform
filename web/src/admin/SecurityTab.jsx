import { getAdminSecurityEvents } from '../api.js'
import { formatDate } from '../time.js'
import { useAsyncData } from '../useAsyncData.js'

const TYPE_LABEL = {
    RATE_LIMITED: '요청 제한',
    LOGIN_FAILED: '로그인 실패',
    UNAUTHORIZED: '로그인 필요(401)',
    FORBIDDEN: '권한 없음(403)',
    INPUT_REJECTED: '입력 필터',
}

/** 숫자 칸마다 "무슨 뜻인지" 한 줄을 붙인다. 숫자만 있으면 높은지 낮은지 알 수 없다. */
const TYPE_HINT = {
    RATE_LIMITED: '짧은 시간에 너무 많이 보내 429 로 막은 횟수',
    LOGIN_FAILED: '이메일·비밀번호가 틀린 로그인',
    UNAUTHORIZED: '로그인 없이 보호된 주소를 부른 횟수',
    FORBIDDEN: '권한이 없는 주소를 부른 횟수',
    INPUT_REJECTED: 'HTML 태그·위험한 주소·제어 문자로 거절된 글',
}

/**
 * 비정상 접근 기록. 서버 메모리에만 있어서 서버를 다시 켜면 비워진다(최근 200건).
 * 같은 사람이 계속 두드려도 1분에 한 줄만 남기고 횟수는 계속 센다.
 */
export default function SecurityTab() {
    const { data, error, loading, reload } = useAsyncData(getAdminSecurityEvents, [])

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

            {error && <p className="admin__error">{error}</p>}
            {loading && !data && <p className="admin__empty">불러오는 중…</p>}

            {data && (
                <>
                    <div className="admin__scroll">
                        <table>
                            <thead>
                                <tr>
                                    <th>종류</th>
                                    <th>횟수</th>
                                    <th>뜻</th>
                                </tr>
                            </thead>
                            <tbody>
                                {Object.keys(TYPE_LABEL).map((type) => (
                                    <tr key={type}>
                                        <td>{TYPE_LABEL[type]}</td>
                                        <td>{(data.counts?.[type] ?? 0).toLocaleString('ko-KR')}</td>
                                        <td>{TYPE_HINT[type]}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    <h3>최근 기록</h3>

                    {data.recent.length === 0 && (
                        <p className="admin__empty">아직 기록이 없습니다. 조용한 것은 좋은 일이에요.</p>
                    )}

                    {data.recent.length > 0 && (
                        <div className="admin__scroll">
                            <table>
                                <thead>
                                    <tr>
                                        <th>시각</th>
                                        <th>종류</th>
                                        <th>접속 IP</th>
                                        <th>주소</th>
                                        <th>내용</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {data.recent.map((event, index) => (
                                        <tr key={`${event.at}-${index}`}>
                                            <td>{new Date(event.at).toLocaleString('ko-KR')}</td>
                                            <td>{TYPE_LABEL[event.type] ?? event.type}</td>
                                            <td>{event.ip}</td>
                                            <td>{event.path}</td>
                                            <td>{event.detail}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </>
            )}
        </div>
    )
}
