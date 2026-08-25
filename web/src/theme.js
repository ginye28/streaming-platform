import { useSyncExternalStore } from 'react'

/**
 * 밝은 모드가 기본이고, 나이트 모드는 사용자가 켠 경우에만 쓴다.
 * OS 설정(prefers-color-scheme)은 일부러 따르지 않는다 — 기본은 항상 밝은 화면이다.
 */
const STORAGE_KEY = 'theme'
const THEMES = ['light', 'dark']

const listeners = new Set()
let current = 'light'

function readStored() {
    try {
        const stored = localStorage.getItem(STORAGE_KEY)
        return THEMES.includes(stored) ? stored : 'light'
    } catch {
        // 시크릿 모드처럼 저장소 접근이 막힌 경우엔 기본값으로 둔다.
        return 'light'
    }
}

function apply(theme) {
    current = theme
    document.documentElement.dataset.theme = theme
}

/** 화면이 한 번 그려지기 전에 불러서 밝은 화면이 깜빡이는 걸 막는다. */
export function initTheme() {
    apply(readStored())
}

export function setTheme(theme) {
    if (!THEMES.includes(theme) || theme === current) {
        return
    }

    apply(theme)

    try {
        localStorage.setItem(STORAGE_KEY, theme)
    } catch {
        // 저장에 실패해도 이번 방문 동안은 적용된 채로 둔다.
    }

    listeners.forEach((notify) => notify())
}

function subscribe(notify) {
    listeners.add(notify)
    return () => listeners.delete(notify)
}

export function useTheme() {
    return useSyncExternalStore(
        subscribe,
        () => current,
        () => 'light'
    )
}
