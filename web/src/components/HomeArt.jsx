/**
 * 빈 화면에 놓는 그림. 방송 화면, 하트, 채팅 말풍선, 시청자 얼굴이 떠 있다.
 * 색은 전부 토큰이라 나이트 모드도 따라간다. 움직임(둥둥, 맥박)은 home.css 가 맡고 '가볍게'에서는 꺼진다.
 */
export default function HomeArt({ className = '' }) {
    return (
        <svg className={`art ${className}`.trim()} viewBox="0 0 480 300" aria-hidden="true" focusable="false">
            <rect className="art__back" x="36" y="28" width="408" height="250" rx="32" />

            <g className="art__screen">
                <rect className="art__frame" x="72" y="54" width="336" height="196" rx="18" />
                <rect className="art__video" x="84" y="66" width="312" height="144" rx="12" />
                <circle className="art__play-ring" cx="240" cy="138" r="30" />
                <circle className="art__play" cx="240" cy="138" r="30" />
                <path className="art__tri" d="M231.5 122.5v31a1.6 1.6 0 0 0 2.4 1.4l25-15.5a1.6 1.6 0 0 0 0-2.7l-25-15.5a1.6 1.6 0 0 0-2.4 1.3z" />
                <g className="art__live">
                    <rect x="96" y="78" width="54" height="22" rx="6" />
                    <circle className="art__dot" cx="109" cy="89" r="3.2" />
                    <text x="117" y="93.5">LIVE</text>
                </g>
                <rect className="art__bar" x="84" y="222" width="150" height="9" rx="4.5" />
                <rect className="art__bar art__bar--short" x="84" y="236" width="96" height="7" rx="3.5" />
                <circle className="art__face" cx="378" cy="226" r="14" />
            </g>

            <g className="art__float art__float--a">
                <rect className="art__chat" x="352" y="34" width="104" height="40" rx="20" />
                <circle className="art__chat-dot" cx="382" cy="54" r="4" />
                <circle className="art__chat-dot" cx="400" cy="54" r="4" />
                <circle className="art__chat-dot" cx="418" cy="54" r="4" />
            </g>

            <g className="art__float art__float--b">
                <circle className="art__heart-bg" cx="52" cy="206" r="26" />
                <path
                    className="art__heart"
                    d="M52 219s-12-7.3-12-16.3a6.9 6.9 0 0 1 12-4.2 6.9 6.9 0 0 1 12 4.2C64 211.7 52 219 52 219z"
                />
            </g>

            <g className="art__float art__float--c">
                <circle className="art__tint art__tint--1" cx="438" cy="148" r="16" />
                <circle className="art__tint art__tint--2" cx="418" cy="172" r="12" />
            </g>
        </svg>
    )
}
