/* 벤토 격자의 항목을 만들고 칸 크기를 정하는 도우미. 카드 그림은 components/Bento.jsx. */

export function liveItems(lives) {
    return (lives ?? []).map((live) => ({ kind: 'live', key: `l${live.id}`, live }))
}

export function videoItems(streams) {
    return (streams ?? []).map((stream) => ({ kind: 'video', key: `v${stream.id}`, stream }))
}

/** 앞의 세 칸은 큰 칸(2x2) · 가로형(2x1) · 가로형, 그 뒤는 작은 칸이고 열 칸에 한 번 가로형이 끼어든다. */
export function tileSize(i) {
    if (i === 0) return 'big'
    if (i < 3) return 'wide'

    return (i - 3) % 10 === 9 ? 'wide' : 'mini'
}
