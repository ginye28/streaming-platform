/**
 * 불러오는 중 표시. 반투명한 나뭇잎 셋이 엇갈려 돌며 빛난다.
 * 움직임 줄이기를 켠 사람에게는 CSS 가 멈춘 채로 보여 준다.
 */
export default function LeafLoader({ size = 24, label }) {
    return (
        <span className="c-loader-wrap" role="status">
            <span className="c-loader" style={{ '--s': `${size}px` }} aria-hidden="true">
                <i />
                <i />
                <i />
            </span>
            {label && <span className="c-loader-label">{label}</span>}
        </span>
    )
}
