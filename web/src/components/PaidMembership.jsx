import { useState } from 'react'
import { dateOnly, startMembershipPayment, usePaymentConfig, won } from '../payments.js'

/**
 * 유료 구독 결제 버튼. 처음 유료로 구독할 때도, 남은 기간 뒤에 이어 붙일 때도 같은 버튼이다.
 * 결제가 꺼져 있으면 아무것도 그리지 않는다(부르는 쪽이 자리표시 버튼을 대신 보인다).
 *
 * 결제수단은 두 갈래다. 카드를 고르면 결제창 안에서 간편결제(토스페이·카카오페이 등)도 고를 수 있고,
 * 계좌이체는 따로 열린다. 어떤 수단이 보이는지는 토스 상점 설정이 정한다.
 */
export default function PaidMembership({ channelId, tier, paidUntil, onFail }) {
    const config = usePaymentConfig()
    const [choosing, setChoosing] = useState(false)
    const [busy, setBusy] = useState(false)

    if (!config?.enabled) return null

    const paid = tier === 'PAID'

    async function pay(method) {
        setBusy(true)

        try {
            await startMembershipPayment(channelId, method)
        } catch (e) {
            // 사용자가 결제창을 닫은 것은 오류가 아니다.
            if (e?.code !== 'USER_CANCEL') onFail(e)
        } finally {
            setBusy(false)
        }
    }

    return (
        <span className="paid-membership">
            {paid && paidUntil && (
                <span className="meta">유료 구독 중 · {dateOnly(paidUntil)}까지</span>
            )}

            {!choosing ? (
                <button
                    type="button"
                    className="subscribe-bar__paid"
                    onClick={() => setChoosing(true)}
                    disabled={busy}
                >
                    {paid
                        ? `${config.periodDays}일 연장 · ${won(config.priceKrw)}`
                        : `유료 구독 · ${won(config.priceKrw)} / ${config.periodDays}일`}
                </button>
            ) : (
                <>
                    <button type="button" onClick={() => pay('CARD')} disabled={busy}>
                        카드 · 간편결제
                    </button>
                    <button type="button" onClick={() => pay('TRANSFER')} disabled={busy}>
                        계좌이체
                    </button>
                    <button type="button" onClick={() => setChoosing(false)} disabled={busy}>
                        닫기
                    </button>
                </>
            )}
        </span>
    )
}
