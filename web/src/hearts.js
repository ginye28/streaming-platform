import { useSyncExternalStore } from 'react'

/**
 * 메인 화면에서 하트를 누른 채널들. 누른 순서가 곧 색 순서이자 줄 서는 순서다.
 *
 * 서버에는 아직 이 개념이 없어서 이 브라우저에만 남긴다.
 * 채널 id 로 기억하므로 같은 채널이 다음에 방송을 켜도 하트가 그대로 붙어 있다.
 */
const STORAGE_KEY = 'sp.hearts'

/** 누른 순서대로 붙는 색. 명도가 서로 달라 색만으로 구분하지 않아도 되게 골랐다. */
export const HEART_COLORS = ['#d33e4a', '#e39a1c', '#3b72d0', '#2f9e68', '#9257d7', '#e0679d', '#1aa3b8', '#8a6d3b']

/** 하트 색 바탕 위의 글자색. 흰색이 4.5:1 을 못 넘는 밝은 색에는 어두운 글자를 쓴다. */
export function heartInk(color) {
    const [r, g, b] = [1, 3, 5]
        .map((i) => parseInt(color.slice(i, i + 2), 16) / 255)
        .map((v) => (v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4))
    const luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b

    return 1.05 / (luminance + 0.05) >= 4.5 ? '#ffffff' : '#161a18'
}

const listeners = new Set()
let current = read()

function read() {
    try {
        const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]')
        return Array.isArray(stored) ? stored.filter((id) => typeof id === 'number') : []
    } catch {
        // 시크릿 모드처럼 저장소가 막혀 있으면 빈 목록으로 시작한다.
        return []
    }
}

function write(next) {
    current = next

    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    } catch {
        // 저장이 안 돼도 이번 화면에서는 동작한다.
    }

    listeners.forEach((listener) => listener())
}

export function toggleHeart(channelId) {
    write(
        current.includes(channelId)
            ? current.filter((id) => id !== channelId)
            : [...current, channelId]
    )
}

export function useHearts() {
    return useSyncExternalStore(
        (listener) => {
            listeners.add(listener)
            return () => listeners.delete(listener)
        },
        () => current
    )
}
