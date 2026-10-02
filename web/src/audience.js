/** 방송·채팅을 누가 쓸 수 있는지. 서버의 Audience 와 같은 값이다. */
export const AUDIENCE_OPTIONS = [
    { value: 'ALL', label: '누구나' },
    { value: 'SUBSCRIBERS', label: '구독자만' },
    { value: 'PAID', label: '유료 구독자만' },
]

export const AUDIENCE_LABEL = Object.fromEntries(AUDIENCE_OPTIONS.map(({ value, label }) => [value, label]))

/** 슬로우 모드로 고를 수 있는 대기 시간(초). 서버가 같은 값만 받는다. 0 은 끔. */
export const SLOW_MODE_CHOICES = [0, 3, 5, 10, 30, 60]
