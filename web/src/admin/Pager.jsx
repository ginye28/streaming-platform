/** 서버가 돌려주는 PageResponse 를 그대로 받아 앞뒤로 넘긴다. */
export default function Pager({ page, onChange }) {
    if (!page || page.totalPages <= 1) {
        return null
    }

    return (
        <div className="admin__pager">
            <button disabled={page.page === 0} onClick={() => onChange(page.page - 1)}>
                이전
            </button>

            <span>
                {page.page + 1} / {page.totalPages} (총 {page.totalElements}건)
            </span>

            <button disabled={page.last} onClick={() => onChange(page.page + 1)}>
                다음
            </button>
        </div>
    )
}
