import { useEffect, useRef } from 'react'

/**
 * 마우스를 따라오는 작고 투명한 나뭇잎 둘. 기본 커서는 그대로 두고 꼬리만 더한다.
 * 터치 화면, 마우스가 없는 기기, 움직임 줄이기를 켠 사람에게는 아무것도 그리지 않는다.
 */
export default function LeafCursor() {
    const rootRef = useRef(null)
    const leadRef = useRef(null)
    const trailRef = useRef(null)

    useEffect(() => {
        const fine = window.matchMedia('(hover: hover) and (pointer: fine)').matches
        const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
        const root = rootRef.current
        if (!fine || reduce || !root) return undefined

        let tx = -100, ty = -100, lx = -100, ly = -100, rx = -100, ry = -100, angle = 0, raf = 0

        const onMove = (event) => {
            tx = event.clientX
            ty = event.clientY
            root.classList.add('c-cursor--on')
            root.classList.toggle('c-cursor--link', Boolean(event.target.closest?.('a, button, [role="slider"], input, summary')))
        }
        const onLeave = () => root.classList.remove('c-cursor--on')

        const tick = () => {
            lx += (tx - lx) * 0.32
            ly += (ty - ly) * 0.32
            rx += (tx - rx) * 0.13
            ry += (ty - ry) * 0.13

            const vx = tx - lx
            const vy = ty - ly
            if (Math.hypot(vx, vy) > 1.2) angle = (Math.atan2(vy, vx) * 180) / Math.PI + 45

            leadRef.current.style.transform = `translate(${lx}px, ${ly}px) rotate(${angle}deg)`
            trailRef.current.style.transform = `translate(${rx}px, ${ry}px) rotate(${angle - 25}deg)`
            raf = requestAnimationFrame(tick)
        }

        window.addEventListener('pointermove', onMove)
        document.addEventListener('pointerleave', onLeave)
        raf = requestAnimationFrame(tick)

        return () => {
            window.removeEventListener('pointermove', onMove)
            document.removeEventListener('pointerleave', onLeave)
            cancelAnimationFrame(raf)
        }
    }, [])

    return (
        <div className="c-cursor" ref={rootRef} aria-hidden="true">
            <i className="c-cursor__trail" ref={trailRef} />
            <i className="c-cursor__lead" ref={leadRef} />
        </div>
    )
}
