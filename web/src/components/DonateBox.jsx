import { useState } from 'react'
import { donationTier } from '../donation.js'
import { startDonationPayment, usePaymentConfig, won } from '../payments.js'

/**
 * 방송 후원(슈퍼챗) 결제 칸. 금액을 고르고 하고 싶은 말을 적은 뒤 결제수단을 고르면 결제창이 열린다.
 * 결제가 꺼져 있으면 아무것도 그리지 않는다.
 *
 * 결제창으로 넘어가기 전에 서버가 주문을 만든다. 그 단계에서 방송 중인지, 허용된 금액인지, 내가 지금 채팅할 수
 * 있는지를 확인하므로, 막힌 사람은 돈을 내기 전에 이유를 알려 주는 오류를 받는다.
 */
export default function DonateBox({ liveId, onFail }) {
    const config = usePaymentConfig()
    const [open, setOpen] = useState(false)
    const [amount, setAmount] = useState(null)
    const [message, setMessage] = useState('')
    const [busy, setBusy] = useState(false)

    if (!config?.enabled || !config.donationAmounts?.length) return null

    async function pay(method) {
        if (!amount) return

        setBusy(true)

        try {
            await startDonationPayment(liveId, amount, message.trim(), method)
        } catch (e) {
            // 사용자가 결제창을 닫은 것은 오류가 아니다.
            if (e?.code !== 'USER_CANCEL') onFail(e.message)
        } finally {
            setBusy(false)
        }
    }

    if (!open) {
        return (
            <div className="donate">
                <button type="button" className="donate__open" onClick={() => setOpen(true)}>
                    후원하기
                </button>
            </div>
        )
    }

    return (
        <div className="donate donate--open">
            <div className="donate__amounts" role="group" aria-label="후원 금액">
                {config.donationAmounts.map((value) => (
                    <button
                        key={value}
                        type="button"
                        className={`donate__amount donate__amount--t${donationTier(value)}`}
                        aria-pressed={amount === value}
                        onClick={() => setAmount(value)}
                    >
                        {won(value)}
                    </button>
                ))}
            </div>

            <input
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                maxLength={config.donationMessageMaxLength}
                placeholder="함께 남길 말 (선택)"
                aria-label="후원 메시지"
            />

            <div className="donate__actions">
                <button type="button" onClick={() => pay('CARD')} disabled={busy || !amount}>
                    카드 · 간편결제
                </button>
                <button type="button" onClick={() => pay('TRANSFER')} disabled={busy || !amount}>
                    계좌이체
                </button>
                <button type="button" onClick={() => setOpen(false)} disabled={busy}>
                    닫기
                </button>
            </div>

            <p className="meta">후원은 직접 환불할 수 없습니다. 문제가 있으면 문의해 주세요.</p>
        </div>
    )
}
