import { navigate, toSearch } from '../router.js'

/** 새 탭 열기·주소 복사가 되도록 진짜 <a> 를 쓰되, 좌클릭은 가로채 새로고침 없이 이동한다. */
export default function Link({ to, children, ...rest }) {
    function handleClick(event) {
        if (event.metaKey || event.ctrlKey || event.shiftKey || event.button !== 0) {
            return
        }

        event.preventDefault()
        navigate(to)
    }

    return (
        <a href={toSearch(to)} onClick={handleClick} {...rest}>
            {children}
        </a>
    )
}
