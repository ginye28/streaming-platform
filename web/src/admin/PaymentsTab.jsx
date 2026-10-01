import { useState } from 'react'
import { adminCancelPayment, getAdminPayments } from '../api.js'
import Pager from '../components/Pager.jsx'
import { useAsyncData } from '../useAsyncData.js'

const STATUS_LABEL = { DONE: '승인', CANCELED: '환불됨', FAILED: '실패', READY: '대기' }

/** 결제 목록과 환불. 사용자 환불 가능 기간이 지난 결제도 사유를 적으면 환불할 수 있다. */
export default function PaymentsTab() {
    const [status, setStatus] = useState('')
    const [pageNumber, setPageNumber] = useState(0)
    const [target, setTarget] = useState(null)
    const [reason, setReason] = useState('')

    const {
        data: page,
        error,
        loading,
        reload,
        fail,
    } = useAsyncData(() => getAdminPayments(status, pageNumber), [status, pageNumber])

    async function handleCancel(event) {
        event.preventDefault()

        try {
            await adminCancelPayment(target.id, reason)
            setTarget(null)
            setReason('')
            reload()
        } catch (e) {
            fail(e)
        }
    }

    return (
        <div className="admin__panel">
            <div className="admin__toolbar">
                <label htmlFor="payment-status">상태</label>

                <select
                    id="payment-status"
                    value={status}
                    onChange={(e) => {
                        setStatus(e.target.value)
                        setPageNumber(0)
                    }}
                >
                    <option value="">전체</option>
                    <option value="DONE">승인</option>
                    <option value="CANCELED">환불됨</option>
                    <option value="FAILED">실패</option>
                    <option value="READY">대기</option>
                </select>

                <button onClick={reload} disabled={loading}>
                    새로고침
                </button>
            </div>

            {error && <p className="admin__error">{error}</p>}

            {target && (
                <form className="admin__toolbar" onSubmit={handleCancel}>
                    <span>
                        #{target.id} {target.userNickname} → {target.channelNickname}{' '}
                        {target.amount.toLocaleString('ko-KR')}원 환불 사유
                    </span>
                    <input
                        value={reason}
                        onChange={(e) => setReason(e.target.value)}
                        maxLength={200}
                        placeholder="예: 이중 결제 확인"
                        required
                    />
                    <button type="submit">환불하기</button>
                    <button type="button" onClick={() => setTarget(null)}>
                        취소
                    </button>
                </form>
            )}

            {loading && <p className="admin__empty">불러오는 중…</p>}

            {!loading && page?.content.length === 0 && (
                <p className="admin__empty">해당하는 결제가 없습니다.</p>
            )}

            {!loading && page?.content.length > 0 && (
                <div className="admin__scroll">
                    <table>
                        <thead>
                            <tr>
                                <th>번호</th>
                                <th>결제한 사람</th>
                                <th>채널</th>
                                <th>금액</th>
                                <th>상태</th>
                                <th>수단</th>
                                <th>승인</th>
                                <th>비고</th>
                                <th>처리</th>
                            </tr>
                        </thead>

                        <tbody>
                            {page.content.map((payment) => (
                                <tr key={payment.id}>
                                    <td>{payment.id}</td>
                                    <td>
                                        {payment.userNickname}
                                        <br />
                                        <span className="admin__who">{payment.userEmail}</span>
                                    </td>
                                    <td>{payment.channelNickname}</td>
                                    <td>{payment.amount.toLocaleString('ko-KR')}원</td>
                                    <td>{STATUS_LABEL[payment.status] ?? payment.status}</td>
                                    <td>{payment.method}</td>
                                    <td>{formatDate(payment.approvedAt)}</td>
                                    <td>
                                        {payment.cancelReason ??
                                            payment.failureMessage ??
                                            ''}
                                    </td>
                                    <td>
                                        {payment.status === 'DONE' && (
                                            <button onClick={() => setTarget(payment)}>환불</button>
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

function formatDate(value) {
    return value ? value.replace('T', ' ').slice(0, 16) : ''
}
