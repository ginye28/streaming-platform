import { useState } from 'react'
import { toggleSubscribe, updateSubscription } from '../api.js'
import { useAuth } from '../useAuth.js'
import { usePaymentConfig } from '../payments.js'
import PaidMembership from './PaidMembership.jsx'
import Link from './Link.jsx'
import SubscriptionControls from './SubscriptionControls.jsx'

/**
 * 채널 구독 — 무료로 구독할지, 유료로 구독할지를 여기서 바로 고른다.
 * 영상·방송·채널 어느 화면에서든 같은 모양으로 붙는다.
 *
 * channel 은 서버가 돌려주는 채널 응답(id · subscribedByMe · myTier · myMarkVisible).
 * 바뀌고 나면 onChanged 로 알려, 부르는 쪽이 채널을 다시 받아 오게 한다.
 */
export default function SubscribeBar({ channel, onChanged, onFail }) {
    const { me } = useAuth()
    const paymentConfig = usePaymentConfig()
    const [busy, setBusy] = useState(false)

    // 내 채널은 구독하는 곳이 아니다.
    if (me?.id === channel.id) return null

    if (!me) {
        return (
            <div className="subscribe-bar">
                <Link to={{ view: 'auth' }} className="cta">
                    로그인하고 구독하기
                </Link>
            </div>
        )
    }

    async function run(action) {
        setBusy(true)

        try {
            await action()
            onChanged()
        } catch (e) {
            onFail(e)
        } finally {
            setBusy(false)
        }
    }

    /** 서버의 구독은 토글이라 구독한 뒤에 유료로 올린다. 처음부터 유료를 고른 경우다. */
    const subscribe = (paid) =>
        run(async () => {
            await toggleSubscribe(channel.id)

            if (paid) {
                await updateSubscription(channel.id, { tier: 'PAID' })
            }
        })

    function unsubscribe() {
        const paidNote = channel.myTier === 'PAID' ? ' 유료 구독도 함께 해지됩니다.' : ''

        if (!window.confirm(`구독을 취소할까요?${paidNote}`)) return

        run(() => toggleSubscribe(channel.id))
    }

    if (!channel.subscribedByMe) {
        // 결제가 켜져 있으면 유료는 결제로만 시작된다. 결제가 끝나면 구독도 함께 만들어진다.
        if (paymentConfig?.enabled) {
            return (
                <div className="subscribe-bar">
                    <button type="button" onClick={() => subscribe(false)} disabled={busy}>
                        무료 구독
                    </button>
                    <PaidMembership channelId={channel.id} tier={null} paidUntil={null} onFail={onFail} />
                    <span className="meta">
                        유료로 구독하면 채널이 정한 특별한 오시마크를 답니다
                    </span>
                </div>
            )
        }

        return (
            <div className="subscribe-bar">
                <button type="button" onClick={() => subscribe(false)} disabled={busy}>
                    무료 구독
                </button>
                <button
                    type="button"
                    className="subscribe-bar__paid"
                    onClick={() => subscribe(true)}
                    disabled={busy}
                >
                    유료 구독
                </button>
                <span className="meta">
                    유료로 구독하면 채널이 정한 특별한 오시마크를 답니다 · 결제 연동 전이라 바로
                    적용됩니다
                </span>
            </div>
        )
    }

    return (
        <div className="subscribe-bar">
            <SubscriptionControls
                channelId={channel.id}
                tier={channel.myTier}
                paidUntil={channel.myPaidUntil}
                markVisible={channel.myMarkVisible}
                onChanged={onChanged}
                onFail={onFail}
            />
            <button type="button" onClick={unsubscribe} disabled={busy}>
                구독 취소
            </button>
        </div>
    )
}
