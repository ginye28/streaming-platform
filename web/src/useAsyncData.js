import { useCallback, useEffect, useState } from 'react'

/**
 * deps 가 바뀌거나 reload() 를 부르면 fetcher 를 다시 호출한다.
 *
 * 상태 갱신은 전부 프라미스 콜백 안에서 한다. 이펙트 본문에서 setState 를 동기로
 * 호출하면 렌더가 연쇄로 일어나 React 가 경고한다.
 */
export function useAsyncData(fetcher, deps) {
    const [state, setState] = useState({ data: null, error: null, loading: true })
    const [reloadKey, setReloadKey] = useState(0)

    useEffect(() => {
        let cancelled = false
        const controller = new AbortController()

        fetcher(controller.signal).then(
            (data) => !cancelled && setState((prev) => ({ data, error: null, loading: false })),
            (error) => {
                if (!cancelled && error.name !== 'AbortError') {
                    setState((prev) => ({ ...prev, error: error.message, loading: false }))
                }
            }
        )

        return () => {
            cancelled = true
            controller.abort()
        }
        // fetcher 는 매 렌더 새로 만들어지므로 의존성에서 뺀다. 갱신 조건은 deps 가 정한다.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [...deps, reloadKey])

    const reload = useCallback(() => setReloadKey((key) => key + 1), [])

    const fail = useCallback(
        (error) => setState((prev) => ({ ...prev, error: error.message })),
        []
    )

    return { ...state, reload, fail }
}
