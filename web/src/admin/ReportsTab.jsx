import { useState } from 'react'
import { getReports, updateReportStatus } from '../api.js'
import Pager from '../components/Pager.jsx'
import { useAsyncData } from '../useAsyncData.js'

const STATUS_LABEL = {
    PENDING: '접수됨',
    RESOLVED: '처리됨',
    REJECTED: '반려됨',
}

const TARGET_LABEL = {
    STREAM: '영상',
    LIVE_STREAM: '라이브',
    COMMENT: '댓글',
    USER: '사용자',
}

export default function ReportsTab() {
    const [status, setStatus] = useState('PENDING')
    const [pageNumber, setPageNumber] = useState(0)

    const {
        data: page,
        error,
        loading,
        reload,
        fail,
    } = useAsyncData(() => getReports(status, pageNumber), [status, pageNumber])

    async function handleStatusChange(id, next) {
        try {
            await updateReportStatus(id, next)
            reload()
        } catch (e) {
            fail(e)
        }
    }

    return (
        <div className="admin__panel">
            <div className="admin__toolbar">
                <label htmlFor="report-status">상태</label>

                <select
                    id="report-status"
                    value={status}
                    onChange={(e) => {
                        setStatus(e.target.value)
                        setPageNumber(0)
                    }}
                >
                    <option value="">전체</option>
                    <option value="PENDING">접수됨</option>
                    <option value="RESOLVED">처리됨</option>
                    <option value="REJECTED">반려됨</option>
                </select>

                <button onClick={reload} disabled={loading}>
                    새로고침
                </button>
            </div>

            {error && <p className="admin__error">{error}</p>}

            {loading && <p className="admin__empty">불러오는 중…</p>}

            {!loading && page?.content.length === 0 && (
                <p className="admin__empty">해당하는 신고가 없습니다.</p>
            )}

            {!loading && page?.content.length > 0 && (
                <div className="admin__scroll">
                    <table>
                        <thead>
                            <tr>
                                <th>번호</th>
                                <th>대상</th>
                                <th>사유</th>
                                <th>신고자</th>
                                <th>접수 시각</th>
                                <th>상태</th>
                                <th>처리</th>
                            </tr>
                        </thead>

                        <tbody>
                            {page.content.map((report) => (
                                <tr key={report.id}>
                                    <td>{report.id}</td>
                                    <td>
                                        {TARGET_LABEL[report.targetType] ?? report.targetType} #
                                        {report.targetId}
                                    </td>
                                    <td>{report.reason}</td>
                                    <td>{report.reporterNickname}</td>
                                    <td>{formatDateTime(report.createdAt)}</td>
                                    <td>
                                        <span
                                            className={`admin__badge admin__badge--${report.status.toLowerCase()}`}
                                        >
                                            {STATUS_LABEL[report.status]}
                                        </span>
                                    </td>
                                    <td>
                                        {report.status === 'PENDING' ? (
                                            <>
                                                <button
                                                    onClick={() =>
                                                        handleStatusChange(report.id, 'RESOLVED')
                                                    }
                                                >
                                                    처리
                                                </button>{' '}
                                                <button
                                                    onClick={() =>
                                                        handleStatusChange(report.id, 'REJECTED')
                                                    }
                                                >
                                                    반려
                                                </button>
                                            </>
                                        ) : (
                                            <button
                                                onClick={() =>
                                                    handleStatusChange(report.id, 'PENDING')
                                                }
                                            >
                                                되돌리기
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            <Pager page={page} onChange={setPageNumber} />
        </div>
    )
}

function formatDateTime(value) {
    return value ? value.replace('T', ' ').slice(0, 16) : ''
}
