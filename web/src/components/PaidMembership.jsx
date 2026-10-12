import { useState } from 'react'
import { dateOnly, startMembershipPayment, usePaymentConfig, won } from '../payments.js'

/** 결제 설정에 상품 목록이 없을 때(옛 서버) 쓰는 한 달 상품. */
function fallbackPlans(config) {
    return [{ months: 1, days: config.periodDays, amountKrw: config.priceKrw, discountPercent: 0 }]
}

/** "1개월", "3개월", "1년" 처럼 보기 좋게. */
function monthsLabel(months) {
    return months === 12 ? '1년' : `${months}개월`
}

/**
 * 유료 구독 결제 버튼. 처음 유료로 구독할 때도, 남은 기간 뒤에 이어 붙일 때도 같은 버튼이다.
 * 결제가 꺼져 있으면 아무것도 그리지 않는다(부르는 쪽이 자리표시 버튼을 대신 보인다).
 *
 * 순서는 ① 기간(1·3·6·12개월) 고르기 → ② 결제수단 고르기다.
 * 오래 구독할수록 한 달 값이 싸다. 금액은 서버가 상품에서 정하고, 여기서 보이는 값은 안내일 뿐이다.
 *
 * 결제수단은 두 갈래다. 카드를 고르면 결제창 안에서 간편결제(토스페이·카카오페이 등)도 고를 수 있고,
 * 계좌이체는 따로 열린다. 어떤 수단이 보이는지는 토스 상점 설정이 정한다.
 */
export default function PaidMembership({ channelId, tier, paidUntil, onFail }) {
    const config = usePaymentConfig()
    const [choosing, setChoosing] = useState(false)
    const [months, setMonths] = useState(null)
    const [busy, setBusy] = useState(false)

    if (!config?.enabled) return null

    const plans = config.plans?.length ? config.plans : fallbackPlans(config)
    const first = plans[0]
    const paid = tier === 'PAID'
    const selected = plans.find((plan) => plan.months === (months ?? first.months)) ?? first

    function close() {
        setChoosing(false)
        setMonths(null)
    }

    async function pay(method) {
        setBusy(true)

        try {
            await startMembershipPayment(channelId, method, selected.months)
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
                        ? `기간 연장 · ${won(first.amountKrw)}부터`
                        : `유료 구독 · ${won(first.amountKrw)}부터`}
                </button>
            ) : (
                <span className="plan-picker">
                    <span className="plan-picker__plans" role="radiogroup" aria-label="구독 기간">
                        {plans.map((plan) => (
                            <label
                                key={plan.months}
                                className={`plan-picker__plan${plan.months === selected.months ? ' is-selected' : ''}`}
                            >
                                <input
                                    type="radio"
                                    name={`plan-${channelId}`}
                                    value={plan.months}
                                    checked={plan.months === selected.months}
                                    onChange={() => setMonths(plan.months)}
                                    disabled={busy}
                                />
                                <strong>{monthsLabel(plan.months)}</strong>
                                <span>{won(plan.amountKrw)}</span>
                                {plan.discountPercent > 0 && (
                                    <em className="plan-picker__off">{plan.discountPercent}% 할인</em>
                                )}
                            </label>
                        ))}
                    </span>

                    <span className="plan-picker__summary meta">
                        {monthsLabel(selected.months)}({selected.days}일) · {won(selected.amountKrw)}
                        {selected.months > 1 &&
                            ` · 한 달 ${won(Math.round(selected.amountKrw / selected.months / 100) * 100)} 꼴`}
                    </span>

                    <span className="plan-picker__methods">
                        <button type="button" onClick={() => pay('CARD')} disabled={busy}>
                            카드 · 간편결제
                        </button>
                        <button type="button" onClick={() => pay('TRANSFER')} disabled={busy}>
                            계좌이체
                        </button>
                        <button type="button" onClick={close} disabled={busy}>
                            닫기
                        </button>
                    </span>
                </span>
            )}
        </span>
    )
}
