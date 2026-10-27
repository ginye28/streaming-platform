import { useEffect, useState } from 'react'
import { getCategories, getLives, getSchedules, getStreams } from '../api.js'
import CinemaHome from '../components/CinemaHome.jsx'
import { pruneHearts, useHearts } from '../hearts.js'
import { useAsyncData } from '../useAsyncData.js'
import { useSlow } from '../useSlow.js'

const SORTS = [
    { value: 'LATEST', label: '최신순' },
    { value: 'POPULAR', label: '인기순' },
]

/**
 * 대회 중계 채널. 이 채널들 중 하나라도 방송 중이면 맨 위 큰 화면에 먼저 올린다.
 * 서버에 대회 개념이 생기기 전까지는 배포 설정(VITE_TOURNAMENT_CHANNEL_IDS)으로 정한다.
 */
const TOURNAMENT_CHANNEL_IDS = (import.meta.env.VITE_TOURNAMENT_CHANNEL_IDS ?? '')
    .split(',')
    .map((value) => Number(value.trim()))
    .filter((value) => Number.isInteger(value) && value > 0)

/**
 * 홈. 이 화면이 답하는 질문은 하나다: "지금 무엇을 볼까?"
 * 여기서는 데이터만 모으고, 화면은 CinemaHome(시네마 슬라이더 + 벤토 격자)이 그린다.
 */
export default function HomePage() {
    const [categoryId, setCategoryId] = useState('')
    const [sortBy, setSortBy] = useState('LATEST')
    const hearts = useHearts()

    const { data: categories } = useAsyncData(getCategories, [])
    const { data: lives, error: livesError, loading: livesLoading, reload: reloadLives } = useAsyncData(() => getLives(0), [])
    const { data: schedulePage, loading: scheduleLoading } = useAsyncData(() => getSchedules(0), [])
    const videos = useVideos(categoryId, sortBy)

    // 방송이 하나도 없을 때만 가장 많이 본 영상을 대표 카드로 올린다. 방송이 있으면 요청도 보내지 않는다.
    const noLive = !livesLoading && !livesError && (lives?.content ?? []).length === 0
    const { data: popularPage, loading: popularLoading } = useAsyncData(
        () => (noLive ? getStreams('', 'POPULAR', 0) : Promise.resolve(null)),
        [noLive]
    )
    const popular = popularPage?.content ?? []

    // 무료 서버는 한참 안 쓰면 잠든다. 첫 요청이 오래 걸리면 그 이유를 알려 준다.
    const waking = useSlow(livesLoading || videos.loading)

    const liveList = lives?.content ?? []

    // 끝난 방송의 하트는 정리한다. 목록을 아직 못 받은 동안(빈 목록)에는 지우지 않는다.
    useEffect(() => {
        if (!livesLoading && !livesError) pruneHearts(liveList.map((live) => live.id))
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [lives, livesLoading, livesError])

    const ordered = orderLives(liveList, hearts)

    return (
        <CinemaHome
            categories={categories}
            categoryId={categoryId}
            onCategory={setCategoryId}
            lives={ordered}
            livesLoading={livesLoading}
            livesError={livesError}
            onRetryLives={reloadLives}
            popular={popular}
            popularLoading={noLive && popularLoading}
            schedules={schedulePage?.content ?? []}
            scheduleLoading={scheduleLoading}
            videos={videos}
            sortBy={sortBy}
            onSort={setSortBy}
            sorts={SORTS}
            waking={waking}
        />
    )
}

/** 대회 중계 → 하트를 누른 순서 → 시청자 많은 순. */
function orderLives(lives, hearts) {
    const tournament = lives.filter((live) => TOURNAMENT_CHANNEL_IDS.includes(live.channelId))
    const rest = lives.filter((live) => !TOURNAMENT_CHANNEL_IDS.includes(live.channelId))
    const hearted = hearts.map((id) => rest.find((live) => live.id === id)).filter(Boolean)
    const others = rest.filter((live) => !hearts.includes(live.id)).sort((a, b) => b.viewerCount - a.viewerCount)

    return [...tournament, ...hearted, ...others]
}

/**
 * 영상 목록. 카테고리나 정렬이 바뀌면 처음부터 다시 받고, "더 보기"로 다음 쪽을 이어 붙인다.
 * 처음 한 쪽을 받는 동안(또는 조건이 바뀐 직후)은 loading 이다.
 */
function useVideos(categoryId, sortBy) {
    const key = `${categoryId}|${sortBy}`
    const [state, setState] = useState({ key: '', items: [], page: 0, last: true, error: null })
    const [more, setMore] = useState({ loading: false, error: null })
    const [attempt, setAttempt] = useState(0)

    useEffect(() => {
        let alive = true

        getStreams(categoryId, sortBy, 0).then(
            (page) => alive && setState({ key, items: page.content, page: 0, last: page.last, error: null }),
            (e) => alive && setState({ key, items: [], page: 0, last: true, error: e.message })
        )

        return () => {
            alive = false
        }
    }, [categoryId, sortBy, key, attempt])

    function loadMore() {
        setMore({ loading: true, error: null })

        getStreams(categoryId, sortBy, state.page + 1).then(
            (page) => {
                setState((prev) => {
                    if (prev.key !== key) return prev

                    const seen = new Set(prev.items.map((item) => item.id))

                    return {
                        ...prev,
                        items: [...prev.items, ...page.content.filter((item) => !seen.has(item.id))],
                        page: prev.page + 1,
                        last: page.last,
                    }
                })
                setMore({ loading: false, error: null })
            },
            (e) => setMore({ loading: false, error: e.message })
        )
    }

    function retry() {
        setState((prev) => ({ ...prev, key: '', error: null }))
        setAttempt((n) => n + 1)
    }

    return {
        items: state.items,
        loading: state.key !== key,
        error: state.error,
        last: state.last,
        more,
        loadMore,
        retry,
    }
}
