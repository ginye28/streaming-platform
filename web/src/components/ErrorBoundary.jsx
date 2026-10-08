import { Component } from 'react'

/**
 * 화면 하나가 예기치 못한 오류로 무너져도 사이트 전체가 흰 화면이 되지 않게 한다.
 * 머리 줄과 메뉴는 그대로 두고, 그 자리에서 무슨 일이 일어났는지와 다음 행동을 알려 준다.
 */
export default class ErrorBoundary extends Component {
    state = { failed: false }

    static getDerivedStateFromError() {
        return { failed: true }
    }

    componentDidCatch(error) {
        console.error('화면을 그리다 오류가 났습니다.', error)
    }

    render() {
        if (!this.state.failed) return this.props.children

        return (
            <div className="empty" role="alert">
                <p>화면을 불러오지 못했어요. 잠시 뒤에 다시 시도해 주세요.</p>
                <button type="button" onClick={() => window.location.reload()}>
                    새로고침
                </button>
            </div>
        )
    }
}
