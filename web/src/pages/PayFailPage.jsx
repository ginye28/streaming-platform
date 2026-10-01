import Link from '../components/Link.jsx'

/** 토스 결제창에서 결제가 안 됐거나 닫았을 때 돌아오는 곳. 주소의 message · code 를 그대로 보여 준다. */
export default function PayFailPage() {
    const params = new URLSearchParams(window.location.search)
    const message = params.get('message') ?? '결제가 취소되었거나 완료되지 않았습니다.'
    const code = params.get('code')

    return (
        <section className="pay-result">
            <h2>결제하지 못했습니다</h2>
            <p className="error">{message}</p>
            {code && <p className="meta">오류 코드 {code}</p>}
            <p className="meta">결제되지 않았으므로 금액은 청구되지 않습니다.</p>
            <p>
                <Link to={{ view: 'home' }}>홈으로</Link>
            </p>
        </section>
    )
}
