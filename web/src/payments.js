import { useEffect, useState } from 'react'
import { createMembershipOrder, getPaymentConfig } from './api.js'

const SDK_URL = 'https://js.tosspayments.com/v2/standard'

let configPromise = null

/** 결제 설정은 자주 바뀌지 않으니 한 번만 받아 둔다. 실패하면 다음에 다시 받는다. */
export function loadPaymentConfig() {
    if (!configPromise) {
        configPromise = getPaymentConfig().catch(() => {
            configPromise = null
            return { enabled: false }
        })
    }

    return configPromise
}

/** 결제가 켜져 있는지 알려 주는 훅. 받아 오는 동안은 null. */
export function usePaymentConfig() {
    const [config, setConfig] = useState(null)

    useEffect(() => {
        let alive = true

        loadPaymentConfig().then((value) => alive && setConfig(value))

        return () => {
            alive = false
        }
    }, [])

    return config
}

export const won = (amount) => `${Number(amount).toLocaleString('ko-KR')}원`

/** 서버가 주는 "2026-11-01T12:00:00" 에서 날짜만. */
export const dateOnly = (value) => (value ? value.slice(0, 10) : '')

let sdkPromise = null

/** 토스 결제 SDK 는 결제를 누를 때만 불러온다. 결제와 상관없는 화면은 가볍게 둔다. */
function loadSdk() {
    if (window.TossPayments) return Promise.resolve(window.TossPayments)

    if (!sdkPromise) {
        sdkPromise = new Promise((resolve, reject) => {
            const script = document.createElement('script')
            script.src = SDK_URL
            script.onload = () => resolve(window.TossPayments)
            script.onerror = () => {
                sdkPromise = null
                reject(new Error('결제 모듈을 불러오지 못했습니다. 잠시 뒤 다시 시도해 주세요.'))
            }
            document.head.appendChild(script)
        })
    }

    return sdkPromise
}

/**
 * 유료 구독 결제창을 연다. 성공하면 토스가 ?view=pay-result 로 되돌려 보내고, 거기서 승인한다.
 * 이 함수가 끝났다는 것은 결제창으로 넘어갔다는 뜻이다(보통은 그 전에 페이지가 떠난다).
 *
 * @param method 'CARD'(카드 · 간편결제) 또는 'TRANSFER'(계좌이체)
 * @throws 사용자가 결제창을 닫으면 code 가 USER_CANCEL 인 오류. 호출하는 쪽에서 조용히 넘긴다.
 */
export async function startMembershipPayment(channelId, method) {
    const config = await loadPaymentConfig()

    if (!config.enabled) {
        throw new Error('결제가 아직 설정되지 않았습니다.')
    }

    // 금액·주문번호는 서버가 정한 값을 그대로 쓴다. 브라우저가 정하지 않는다.
    const order = await createMembershipOrder(channelId)

    const TossPayments = await loadSdk()
    const payment = TossPayments(config.clientKey).payment({
        customerKey: TossPayments.ANONYMOUS ?? '@@ANONYMOUS',
    })

    const origin = window.location.origin

    await payment.requestPayment({
        method,
        amount: { currency: 'KRW', value: order.amount },
        orderId: order.orderId,
        orderName: order.orderName,
        successUrl: `${origin}/?view=pay-result`,
        failUrl: `${origin}/?view=pay-fail`,
        customerEmail: order.customerEmail,
        customerName: order.customerName,
    })
}
