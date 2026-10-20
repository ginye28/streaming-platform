/** 이름(또는 id)을 0~7 의 고정된 번호로. 같은 값은 항상 같은 번호라, 같은 사람은 어디서나 같은 색조다. */
export function tintOf(value) {
    const text = String(value ?? '')
    let hash = 0

    for (let i = 0; i < text.length; i += 1) {
        hash = (hash * 31 + text.charCodeAt(i)) >>> 0
    }

    return hash % 8
}
