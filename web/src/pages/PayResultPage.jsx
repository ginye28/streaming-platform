import { confirmPayment } from '../api.js'
import { dateOnly, won } from '../payments.js'
import Link from '../components/Link.jsx'
import { loginTo } from '../router.js'
import { useAsyncData } from '../useAsyncData.js'
import { useAuth } from '../useAuth.js'

/**
 * 토스 결제창에서 돌아오는 곳. 주소에 붙어 온 paymentKey · orderId · amount 로 서버에 승인을 요청한다.
 *
 * 주소의 값은 사용자가 고칠 수 있으므로 믿지 않는다. 서버가 주문과 대조해서 맞을 때만 승인한다.
 * 새로고침해도 같은 결제를 두 번 청구하지 않는다(서버가 같은 주문은 한 번만 처리한다).
 */
export default function PayResultPage({ params = new URLSearchParams(window.location.search) }) {
    const paymentKey = params.get('paymentKey')
    const orderId = params.get('orderId')
    const amount = Number(params.get('amount'))

    // 결제사가 돌려보낸 값이 하나라도 없으면 승인을 요청하지 않는다(서버에 빈 값을 보내 오류만 받게 된다).
    const complete = Boolean(paymentKey && orderId && Number.isFinite(amount) && amount > 0)

    const { me, loading: authLoading } = useAuth()

    // 로그인이 풀린 채(다른 기기에서 결제를 마친 경우 등) 돌아왔다면 먼저 로그인하게 한다. 로그인하면 이 화면으로 돌아와 승인을 잇는다.
    const needLogin = complete && !authLoading && !me

    const { data: result, error, loading } = useAsyncData(
        () =>
            complete && !authLoading && me
                ? confirmPayment({ paymentKey, orderId, amount })
                : Promise.resolve(null),
        [authLoading, Boolean(me)]
    )

    if (!complete) {
        return (
            <section className="pay-result">
                <h2>결제 정보를 찾지 못했습니다</h2>
                <p className="meta">
                    결제 화면에서 돌아온 주소에 결제 정보가 없습니다. 결제가 이미 끝났을 수 있으니 결제 내역에서 먼저
                    확인해 주세요. 결제되지 않았다면 금액은 청구되지 않습니다.
                </p>
                <p>
                    <Link to={{ view: 'me' }}>내 계정에서 결제 내역 보기</Link>
                </p>
            </section>
        )
    }

    if (needLogin) {
        return (
            <section className="pay-result">
                <h2>로그인하면 결제를 마무리합니다</h2>
                <p className="meta">
                    결제는 끝났지만 이 기기에서는 로그인이 되어 있지 않습니다. 결제할 때 쓴 계정으로 로그인하면 바로
                    이어서 확인합니다.
                </p>
                <p>
                    <Link to={loginTo()}>로그인하기</Link>
                </p>
            </section>
        )
    }

    if (loading || authLoading || !result) {
        if (!error) {
            return <p className="empty">결제를 확인하는 중입니다. 이 화면을 닫지 마세요…</p>
        }
    }

    if (error) {
        return (
            <section className="pay-result">
                <h2>결제를 완료하지 못했습니다</h2>
                <p className="error">{error}</p>
                <p className="meta">
                    결제 금액이 빠져나갔는데 유료 구독이나 후원이 반영되지 않았다면, 이 화면을 새로고침하면 다시
                    확인합니다. 이미 반영됐을 수도 있으니 <strong>내 계정의 결제 내역</strong>도 확인해 주세요.
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
