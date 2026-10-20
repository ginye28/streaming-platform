/** 불러오는 중 표시. 화면을 가리지 않는 작은 고리 하나. 글을 같이 주면 스크린리더에도 읽힌다. */
export default function Spinner({ size = 22, label }) {
    return (
        <span className="spinner-wrap" role="status">
            <span className="spinner" style={{ '--s': `${size}px` }} aria-hidden="true" />
            {label && <span className="spinner-wrap__label">{label}</span>}
        </span>
    )
}
