/**
 * 사용자가 적은 주소를 링크(<a href>)로 쓰기 전에 거른다.
 * http(s) 주소만 통과시키고, javascript: · data: 같은 것은 null 을 돌려준다.
 * (그런 주소를 href 에 두면 링크를 누른 사람의 브라우저에서 스크립트가 실행된다.)
 */
export function safeHttpUrl(value) {
    if (typeof value !== 'string') return null

    const trimmed = value.trim()

    if (!trimmed) return null

    // 공백이나 제어 문자가 섞인 주소("java\tscript:" 같은 우회)는 거절한다.
    for (const ch of trimmed) {
        if (/\s/.test(ch) || ch.charCodeAt(0) < 0x20) return null
    }

    try {
        const url = new URL(trimmed)

        if (url.protocol !== 'http:' && url.protocol !== 'https:') return null

        return url.href
    } catch {
        return null
    }
}
