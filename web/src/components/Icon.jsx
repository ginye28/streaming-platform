/**
 * 아이콘 한 벌. 24px 격자에 선 두께 1.75, 끝은 둥글게. 화면 어디서나 같은 손으로 그린 모양을 쓴다.
 * 이모지나 글자 기호로 아이콘을 대신하지 않는다.
 */
const PATHS = {
    search: (
        <>
            <circle cx="11" cy="11" r="6.5" />
            <path d="m20 20-4-4" />
        </>
    ),
    bell: (
        <>
            <path d="M6 9a6 6 0 0 1 12 0c0 6.5 2.5 8 2.5 8h-17S6 15.5 6 9" />
            <path d="M10.2 20.5a2 2 0 0 0 3.6 0" />
        </>
    ),
    upload: (
        <>
            <path d="M12 15.5V4.5" />
            <path d="m7.5 9 4.5-4.5L16.5 9" />
            <path d="M4.5 15v3a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2v-3" />
        </>
    ),
    live: (
        <>
            <circle cx="12" cy="12" r="2" />
            <path d="M16.4 7.6a6.2 6.2 0 0 1 0 8.8" />
            <path d="M7.6 16.4a6.2 6.2 0 0 1 0-8.8" />
            <path d="M19.2 4.8a10.2 10.2 0 0 1 0 14.4" />
            <path d="M4.8 19.2a10.2 10.2 0 0 1 0-14.4" />
        </>
    ),
    home: (
        <>
            <path d="M3.5 11.2 12 4l8.5 7.2" />
            <path d="M5.5 10v9.5h4.5v-5.5h4v5.5h4.5V10" />
        </>
    ),
    heart: <path d="M12 20s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7.2a4.3 4.3 0 0 1 7.5 2.6C19.5 15.4 12 20 12 20z" />,
    user: (
        <>
            <circle cx="12" cy="8.5" r="3.8" />
            <path d="M4.5 20.5a7.5 7.5 0 0 1 15 0" />
        </>
    ),
    moon: <path d="M20 14.2A8.2 8.2 0 1 1 9.8 4 6.6 6.6 0 0 0 20 14.2z" />,
    sun: (
        <>
            <circle cx="12" cy="12" r="3.8" />
            <path d="M12 2.5v2M12 19.5v2M4.6 4.6 6 6M18 18l1.4 1.4M2.5 12h2M19.5 12h2M4.6 19.4 6 18M18 6l1.4-1.4" />
        </>
    ),
    logout: (
        <>
            <path d="M9.5 20.5h-4a2 2 0 0 1-2-2v-13a2 2 0 0 1 2-2h4" />
            <path d="m16 16.5 4.5-4.5L16 7.5" />
            <path d="M20.5 12H9.5" />
        </>
    ),
    gauge: <path d="M13 2.5 4.5 13.5H11l-1 8 8.5-11H12z" />,
    shield: <path d="M12 3 4.5 6v5.8c0 4.4 3.1 7.6 7.5 9.2 4.4-1.6 7.5-4.8 7.5-9.2V6z" />,
    close: <path d="M18 6 6 18M6 6l12 12" />,
    back: (
        <>
            <path d="M19 12H5" />
            <path d="m11.5 18.5-6.5-6.5 6.5-6.5" />
        </>
    ),
    calendar: (
        <>
            <rect x="3.5" y="5" width="17" height="15.5" rx="2.5" />
            <path d="M3.5 10h17M8 3v4M16 3v4" />
        </>
    ),
    eye: (
        <>
            <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z" />
            <circle cx="12" cy="12" r="2.8" />
        </>
    ),
    chevron: <path d="m6.5 9.5 5.5 5.5 5.5-5.5" />,
    right: <path d="m9.5 5.5 6.5 6.5-6.5 6.5" />,
    left: <path d="m14.5 5.5-6.5 6.5 6.5 6.5" />,
    check: <path d="m5 12.5 4.5 4.5L19 7.5" />,
}

export default function Icon({ name, size = 22, filled = false, className, ...rest }) {
    return (
        <svg
            className={className}
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill={filled ? 'currentColor' : 'none'}
            stroke="currentColor"
            strokeWidth="1.75"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
            focusable="false"
            {...rest}
        >
            {PATHS[name]}
        </svg>
    )
}
