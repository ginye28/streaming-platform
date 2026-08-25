import { setTheme, useTheme } from '../theme.js'

/** 밝은 화면이 기본이고, 누르면 나이트 모드로 바뀐다. 고른 값은 이 브라우저에 남는다. */
export default function ThemeToggle({ className }) {
    const theme = useTheme()
    const dark = theme === 'dark'

    return (
        <button
            type="button"
            className={className}
            onClick={() => setTheme(dark ? 'light' : 'dark')}
            aria-pressed={dark}
            title={dark ? '밝은 화면으로 바꾸기' : '어두운 화면으로 바꾸기'}
        >
            {dark ? '밝게' : '어둡게'}
        </button>
    )
}
