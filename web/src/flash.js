const KEY = 'sp.flash'

/** 화면을 넘어가서 한 번만 보여 줄 안내(예: 영상을 올린 뒤). 저장소를 못 쓰면 안내만 건너뛴다. */
export function setFlash(message) {
    try {
        sessionStorage.setItem(KEY, message)
    } catch {
        // 안내가 빠질 뿐 흐름은 그대로다.
    }
}

export function peekFlash() {
    try {
        return sessionStorage.getItem(KEY)
    } catch {
        return null
    }
}

export function clearFlash() {
    try {
        sessionStorage.removeItem(KEY)
    } catch {
        // 위와 같다.
    }
}
