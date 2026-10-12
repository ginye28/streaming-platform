import { useSyncExternalStore } from 'react'

/**
 * 화면 효과 등급. 유리 질감(backdrop-filter)·빛 번짐·끝없이 도는 움직임은 사양이 낮은 폰·태블릿에서
 * 프레임을 떨어뜨린다. 그래서 기기를 보고(또는 실제로 재 보고) 가벼운 화면으로 바꾼다.
 *
 *   full  기본. 유리 질감과 빛, 움직임을 전부 쓴다.
 *   lite  유리 질감(blur)을 걷어 내고 끝없이 도는 움직임을 멈춘다. 내용과 배치는 그대로다.
 *
 * 사용자가 고른 값(auto / full / lite)이 가장 먼저고, auto 일 때만 기기 힌트와 프레임 측정을 따른다.
 * 결정은 <html data-perf="full|lite"> 로 나가고, CSS 가 그걸 보고 효과를 끈다.
 */

const MODE_KEY = 'sp.perf'
const AUTO_KEY = 'sp.perf.auto'
const MODES = ['auto', 'full', 'lite']

/** 측정으로 "느리다"고 판정한 결과를 기억해 두는 기간. 기기가 바뀌었거나 나아졌을 수 있어서 영원히 두지 않는다. */
const AUTO_TTL_MS = 7 * 24 * 60 * 60 * 1000

/** 이 프레임 간격(ms)보다 느리면(약 30fps 미만) 가벼운 화면으로 바꾼다. 중앙값으로 판정해 순간 끊김에 안 흔들린다. */
const SLOW_FRAME_MS = 34

const PROBE_DELAY_MS = 2500
const PROBE_FRAMES = 90

const listeners = new Set()
let state = { mode: 'auto', level: 'full', reason: '', autoDowngraded: false }

function safeGet(key) {
    try {
        return localStorage.getItem(key)
    } catch {
        return null
    }
}

function safeSet(key, value) {
    try {
        if (value == null) localStorage.removeItem(key)
        else localStorage.setItem(key, value)
    } catch {
        // 저장소가 막혀 있으면 이번 방문 동안만 적용된다.
    }
}

/** 브라우저가 알려 주는 "가벼워야 하는 기기" 신호. 없으면 빈 문자열. */
function deviceHint() {
    const connection = navigator.connection

    if (connection?.saveData) return '데이터 절약 모드'
    if (typeof navigator.deviceMemory === 'number' && navigator.deviceMemory <= 2) return '메모리가 작은 기기'
    if (typeof navigator.hardwareConcurrency === 'number' && navigator.hardwareConcurrency <= 2) return '코어가 적은 기기'

    return ''
}

function recentAutoLite() {
    const stamp = Number(safeGet(AUTO_KEY))

    return Number.isFinite(stamp) && stamp > 0 && Date.now() - stamp < AUTO_TTL_MS
}

function resolve(mode) {
    if (mode === 'lite') return { level: 'lite', reason: '직접 선택' }
    if (mode === 'full') return { level: 'full', reason: '' }

    const hint = deviceHint()
    if (hint) return { level: 'lite', reason: hint }
    if (recentAutoLite()) return { level: 'lite', reason: '이전에 화면이 느렸어요' }

    return { level: 'full', reason: '' }
}

function publish(next) {
    state = next
    document.documentElement.dataset.perf = state.level
    listeners.forEach((notify) => notify())
}

export function getPerf() {
    return state
}

export function setPerfMode(mode) {
    if (!MODES.includes(mode)) return

    safeSet(MODE_KEY, mode === 'auto' ? null : mode)
    // 사용자가 직접 정했으면 이전 측정 결과는 잊는다. "전체" 를 골랐는데 계속 가벼운 화면이면 안 된다.
    safeSet(AUTO_KEY, null)

    publish({ mode, ...resolve(mode), autoDowngraded: false })

    if (mode === 'auto') scheduleProbe()
}

function readStoredMode() {
    // 주소에 ?perf=lite 를 붙이면 그 기기에 그대로 저장된다(기기별 확인용).
    try {
        const fromUrl = new URLSearchParams(window.location.search).get('perf')

        if (MODES.includes(fromUrl)) {
            safeSet(MODE_KEY, fromUrl === 'auto' ? null : fromUrl)
        }
    } catch {
        // 주소를 못 읽으면 저장된 값만 쓴다.
    }

    const stored = safeGet(MODE_KEY)

    return MODES.includes(stored) ? stored : 'auto'
}

/** 화면이 그려지기 전에 불러서, 처음 프레임부터 올바른 등급으로 그린다. */
export function initPerf() {
    const mode = readStoredMode()

    publish({ mode, ...resolve(mode), autoDowngraded: false })

    scheduleProbe()
}

let probeTimer = 0

/** 자동 모드이고 아직 full 이면, 페이지가 가라앉은 뒤 프레임을 재서 느리면 lite 로 내린다. */
function scheduleProbe() {
    clearTimeout(probeTimer)

    if (state.mode !== 'auto' || state.level !== 'full') return
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return

    probeTimer = setTimeout(runProbe, PROBE_DELAY_MS)
}

function runProbe() {
    // 탭이 가려져 있으면 브라우저가 프레임을 멈춰서 측정이 틀어진다. 다시 보일 때 한다.
    if (document.hidden) {
        document.addEventListener('visibilitychange', scheduleProbe, { once: true })
        return
    }

    const deltas = []
    let last = 0

    const tick = (now) => {
        if (document.hidden) {
            document.addEventListener('visibilitychange', scheduleProbe, { once: true })
            return
        }

        if (last) {
            const delta = now - last
            // 탭 전환·디버거 멈춤 같은 아주 긴 간격은 기기 성능이 아니다.
            if (delta < 250) deltas.push(delta)
        }
        last = now

        if (deltas.length < PROBE_FRAMES) {
            requestAnimationFrame(tick)
            return
        }

        deltas.sort((a, b) => a - b)
        const median = deltas[Math.floor(deltas.length / 2)]

        if (median > SLOW_FRAME_MS && state.mode === 'auto' && state.level === 'full') {
            safeSet(AUTO_KEY, String(Date.now()))
            publish({
                mode: 'auto',
                level: 'lite',
                reason: `화면이 느렸어요 (약 ${Math.round(1000 / median)}fps)`,
                autoDowngraded: true,
            })
        }
    }

    requestAnimationFrame(tick)
}

function subscribe(notify) {
    listeners.add(notify)
    return () => listeners.delete(notify)
}

export function usePerf() {
    return useSyncExternalStore(subscribe, getPerf, getPerf)
}

/** 알림을 닫은 뒤에는 같은 알림이 다시 뜨지 않게 표시만 지운다. 등급은 그대로다. */
export function dismissPerfNotice() {
    if (state.autoDowngraded) {
        state = { ...state, autoDowngraded: false }
        listeners.forEach((notify) => notify())
    }
}

/** 주소의 ?fps=1 / ?fps=0 로 계기판(FpsMeter)을 켜고 끄고, 그 기기에 기억해 둔다. */
export function fpsMeterEnabled() {
    try {
        const param = new URLSearchParams(window.location.search).get('fps')

        if (param === '1') localStorage.setItem('sp.fps', '1')
        if (param === '0') localStorage.removeItem('sp.fps')

        return localStorage.getItem('sp.fps') === '1'
    } catch {
        return false
    }
}
