import { confirmPayment } from '../api.js'
import { dateOnly, won } from '../payments.js'
import Link from '../components/Link.jsx'
import { useAsyncData } from '../useAsyncData.js'

/**
 * 토스 결제창에서 돌아오는 곳. 주소에 붙어 온 paymentKey · orderId · amount 로 서버에 승인을 요청한다.
 *
 * 주소의 값은 사용자가 고칠 수 있으므로 믿지 않는다. 서버가 주문과 대조해서 맞을 때만 승인한다.
 * 새로고침해도 같은 결제를 두 번 청구하지 않는다(서버가 같은 주문은 한 번만 처리한다).
 */
export default function PayResultPage() {
    const params = new URLSearchParams(window.location.search)

    const { data: result, error, loading } = useAsyncData(
        () =>
            confirmPayment({
                paymentKey: params.get('paymentKey'),
                orderId: params.get('orderId'),
                amount: Number(params.get('amount')),
            }),
        []
    )

    if (loading) {
        return <p className="empty">결제를 확인하는 중입니다. 이 화면을 닫지 마세요…</p>
    }

    if (error) {
        return (
            <section className="pay-result">
                <h2>결제를 완료하지 못했습니다</h2>
                <p className="error">{error}</p>
                <p className="meta">
                    결제 금액이 빠져나갔는데 유료 구독이나 후원이 반영되지 않았다면, 이 화면을 새로고침하면 다시
                    확인합니다.
                </p>
                <p>
                    <Link to={{ view: 'me' }}>내 계정으로</Link>
                </p>
            </section>
        )
    }

    const donation = result.kind === 'DONATION'

    return (
        <section className="pay-result">
            <h2>{donation ? '후원이 전달되었습니다' : '결제가 완료되었습니다'}</h2>
            {donation ? (
                <p>
                    <strong>{result.channelNickname}</strong> 님의 방송에 후원했습니다. 채팅에 후원 메시지가 올라갑니다.
                </p>
            ) : (
                <p>
                    <strong>{result.channelNickname}</strong> 채널의 유료 구독이{' '}
                    <strong>{dateOnly(result.paidUntil)}</strong>까지 이어집니다.
                </p>
            )}
            <p className="meta">
                {won(result.amount)} · {result.method}
                {result.receiptUrl && (
                    <>
                        {' · '}
                        <a href={result.receiptUrl} target="_blank" rel="noreferrer">
                            영수증
                        </a>
                    </>
                )}
            </p>
            <p>
                {donation && result.liveId ? (
                    <Link to={{ view: 'live', id: result.liveId }}>방송으로 돌아가기</Link>
                ) : (
                    <Link to={{ view: 'channel', id: result.channelId }}>채널로 돌아가기</Link>
                )}
            </p>
        </section>
    )
}
