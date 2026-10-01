import { useState } from 'react'
import { updateSubscription } from '../api.js'

/**
 * 구독 중인 채널에 대한 내 설정 — 오시마크를 이름 옆에 달지, 유료 구독으로 올릴지.
 * 구독했다고 마크를 강제로 달지 않는다. 채널마다 시청자가 고른다.
 */
export default function SubscriptionControls({ channelId, tier, markVisible, onChanged, onFail }) {
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
        </div>
    )
}
