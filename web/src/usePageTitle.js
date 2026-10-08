import { useEffect } from 'react'

const SITE = '스트리밍 플랫폼'

/** 탭 제목을 지금 보는 화면의 이름으로. 떠나면 사이트 이름으로 되돌린다. */
export function usePageTitle(title) {
    useEffect(() => {
        document.title = title ? `${title} · ${SITE}` : SITE

        return () => {
            document.title = SITE
        }
    }, [title])
}
