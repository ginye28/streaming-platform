import { useState } from 'react'
import { updateSubscription } from '../api.js'
import { usePaymentConfig } from '../payments.js'
import PaidMembership from './PaidMembership.jsx'

/**
 * 구독 중인 채널에 대한 내 설정 — 오시마크를 이름 옆에 달지, 유료 구독으로 올릴지.
 * 구독했다고 마크를 강제로 달지 않는다. 채널마다 시청자가 고른다.
 *
 * 유료 구독은 결제가 켜져 있으면 결제로만 시작된다. 결제가 꺼져 있을 때만
 * 결제 없이 바로 바뀌는 자리표시(개발·데모용)가 보인다.
 */
export default function SubscriptionControls({
    channelId,
    tier,
    paidUntil,
    markVisible,
    onChanged,
    onFail,
}) {
    const config = usePaymentConfig()
    const [busy, setBusy] = useState(false)
    const paid = tier === 'PAID'

    async function change(patch) {
        setBusy(true)

        try {
            await updateSubscription(channelId, patch)
            onChanged()
        } catch (e) {
            onFail(e)
        } finally {
            setBusy(false)
        }
    }

    function downgrade() {
        const warning = paidUntil
            ? '유료 구독을 해지할까요? 남은 유료 기간은 사라지고 환불되지 않습니다.'
            : '유료 구독을 해지할까요?'

        if (window.confirm(warning)) change({ tier: 'BASIC' })
    }

    return (
        <div className="subscription-controls">
            <label className="subscription-controls__mark">
                <input
                    type="checkbox"
                    checked={markVisible}
                    disabled={busy}
                    onChange={(event) => change({ markVisible: event.target.checked })}
                />
                내 이름 옆에 오시마크 달기
            </label>

            {config?.enabled ? (
                <>
                    <PaidMembership
                        channelId={channelId}
                        tier={tier}
                        paidUntil={paidUntil}
                        onFail={onFail}
                    />

                    {paid ? (
                        <button type="button" disabled={busy} onClick={downgrade}>
                            유료 해지
                        </button>
                    ) : (
                        <span className="meta">무료 구독 중</span>
                    )}
                </>
            ) : (
                <>
                    <button
                        type="button"
                        disabled={busy}
                        onClick={() => change({ tier: paid ? 'BASIC' : 'PAID' })}
                    >
                        {paid ? '무료 구독으로 변경' : '유료 구독으로 변경'}
                    </button>

                    <span className="meta">
                        {paid ? '유료 구독 중' : '무료 구독 중'} · 결제 연동 전이라 바로 바뀝니다
                    </span>
                </>
            )}
        </div>
    )
}
