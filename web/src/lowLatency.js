/**
 * 저지연 라이브 재생(WebSocket + MSE).
 *
 * 서버(LiveRelay)가 ffmpeg 가 만든 0.1초 단위 fMP4 조각을 받는 즉시 WebSocket 으로 보내 주고,
 * 여기서 MediaSource 에 이어 붙여 방송 끝에 바짝 붙어 재생한다. HLS 는 조각(약 3.4초)이 다 만들어져야
 * 받을 수 있어 지연이 7초 아래로 내려가지 않지만, 이 방식은 1~2초가 목표다.
 *
 * 망이 흔들려 멈추지 않도록 방송 끝에서 {@link TARGET_SECONDS} 만큼만 뒤에서 재생하고,
 * 뒤처지면 살짝 빠르게, 너무 바짝 붙으면 살짝 느리게 틀어 그 간격을 유지한다.
 * 어떤 이유로든 이어 가지 못하면 onFail 로 알려, 호출하는 쪽이 HLS 로 넘어가게 한다.
 */

/** 방송 끝에서 이만큼(초) 뒤에서 재생한다. 작을수록 지연이 줄지만 망이 흔들릴 때 멈추기 쉽다. */
const TARGET_SECONDS = 0.8

/** 이만큼 이상 뒤처지면 건너뛴다(초). */
const SEEK_SECONDS = 3

/** 지난 영상은 이만큼(초)만 들고 있다. 오래 켜 둔 기기의 메모리를 지킨다. */
const KEEP_BEHIND_SECONDS = 6

/** 연결이 끊기면 이만큼 기다렸다 다시 잇는다(ms). 이 횟수를 넘기면 포기한다. */
const RECONNECT_DELAYS = [500, 1000, 2000]

/** 머리나 첫 조각이 이 시간 안에 안 오면 포기한다(ms). */
const FIRST_DATA_TIMEOUT = 10_000

/** 조각이 이 시간 동안 안 오면 끊긴 것으로 본다(ms). 방송 쪽은 0.25초마다 보낸다. */
const STALL_TIMEOUT = 6_000

export function lowLatencySupported() {
    return typeof window !== 'undefined' && 'MediaSource' in window && 'WebSocket' in window
}

/**
 * @param {HTMLVideoElement} video
 * @param {{ url: string, token?: string|null, onReady?: () => void, onFail: (reason: string) => void,
 *           onEnded?: () => void, onStats?: (stats: object) => void }} options
 * @returns {{ stop: () => void }}
 */
export function startLowLatency(video, { url, token, onReady, onFail, onEnded, onStats }) {
    let socket = null
    let mediaSource = null
    let sourceBuffer = null
    let objectUrl = null
    let queue = []
    let codecs = ''
    let stopped = false
    let ready = false
    let reconnects = 0
    let lastData = Date.now()
    let received = 0
    let firstDataTimer = null
    let loopTimer = null
    let appendErrors = 0

    function fail(reason) {
        if (stopped) return

        stop()
        onFail(reason)
    }

    function cleanupMedia() {
        queue = []
        sourceBuffer = null

        if (mediaSource) {
            try {
                if (mediaSource.readyState === 'open') mediaSource.endOfStream()
            } catch {
                // 이미 닫힌 상태
            }
            mediaSource = null
        }

        if (objectUrl) {
            URL.revokeObjectURL(objectUrl)
            objectUrl = null
        }
    }

    function stop() {
        if (stopped) return
        stopped = true

        clearTimeout(firstDataTimer)
        clearInterval(loopTimer)

        if (socket) {
            socket.onopen = socket.onmessage = socket.onclose = socket.onerror = null
            try {
                socket.close()
            } catch {
                // 이미 닫힌 연결
            }
        }

        cleanupMedia()
    }

    function pump() {
        if (!sourceBuffer || sourceBuffer.updating || queue.length === 0 || !mediaSource || mediaSource.readyState !== 'open') {
            return
        }

        try {
            sourceBuffer.appendBuffer(queue.shift())
            appendErrors = 0
        } catch (e) {
            // QuotaExceeded 는 오래된 영상을 지우면 풀린다. 그 밖의 오류가 반복되면 포기한다.
            if (e?.name === 'QuotaExceededError') {
                trim(true)
                return
            }

            appendErrors += 1
            if (appendErrors > 3) fail('append')
        }
    }

    /** 재생 위치 뒤쪽의 오래된 영상을 지운다. */
    function trim(force = false) {
        if (!sourceBuffer || sourceBuffer.updating || sourceBuffer.buffered.length === 0) return

        const start = sourceBuffer.buffered.start(0)
        const keepFrom = video.currentTime - (force ? 2 : KEEP_BEHIND_SECONDS)

        if (keepFrom - start > 1) {
            try {
                sourceBuffer.remove(start, keepFrom)
            } catch {
                // 다음 틱에 다시 시도한다.
            }
        }
    }

    function openMedia() {
        cleanupMedia()

        if (!window.MediaSource.isTypeSupported(`video/mp4; codecs="${codecs}"`)) {
            fail('codec')
            return
        }

        mediaSource = new window.MediaSource()
        objectUrl = URL.createObjectURL(mediaSource)

        mediaSource.addEventListener('sourceopen', () => {
            if (stopped || sourceBuffer) return

            try {
                sourceBuffer = mediaSource.addSourceBuffer(`video/mp4; codecs="${codecs}"`)
            } catch {
                fail('source-buffer')
                return
            }

            sourceBuffer.addEventListener('updateend', () => {
                trim()
                pump()
            })
            sourceBuffer.addEventListener('error', () => fail('buffer-error'))

            pump()
        })

        video.src = objectUrl
    }

    function connect() {
        socket = new WebSocket(url)
        socket.binaryType = 'arraybuffer'

        socket.onopen = () => socket.send(JSON.stringify({ type: 'join', token: token ?? '' }))

        socket.onmessage = (event) => {
            lastData = Date.now()

            if (typeof event.data === 'string') {
                handleText(JSON.parse(event.data))
                return
            }

            received += 1
            queue.push(event.data)
            pump()
        }

        socket.onclose = () => {
            if (stopped) return

            if (reconnects >= RECONNECT_DELAYS.length) {
                fail('closed')
                return
            }

            const delay = RECONNECT_DELAYS[reconnects]
            reconnects += 1

            // 이어 붙이는 도중에 끊겼다. 머리부터 새로 받아야 하므로 재생 상태를 비우고 다시 잇는다.
            cleanupMedia()
            ready = false
            setTimeout(() => !stopped && connect(), delay)
        }

        socket.onerror = () => {
            // onclose 가 이어서 불린다.
        }
    }

    function handleText(message) {
        switch (message.type) {
            case 'init':
                codecs = message.codecs
                openMedia()
                break

            case 'ended':
                onEnded?.()
                break

            case 'unavailable':
            case 'forbidden':
            case 'full':
                fail(message.type)
                break

            default:
        }
    }

    /** 0.25초마다: 방송 끝과의 간격을 목표에 맞춘다. */
    function regulate() {
        if (stopped) return

        if (ready === false && sourceBuffer && sourceBuffer.buffered.length > 0) {
            // 첫 화면: 방송 끝에서 목표만큼 뒤로 가서 재생한다. 늦게 들어와 지난 키프레임부터 받았어도 바로 현재로 간다.
            const end = sourceBuffer.buffered.end(sourceBuffer.buffered.length - 1)
            const start = sourceBuffer.buffered.start(0)

            video.currentTime = Math.max(start, end - TARGET_SECONDS)
            video.play().catch(() => {})
            ready = true
            reconnects = 0
            clearTimeout(firstDataTimer)
            onReady?.()
        }

        if (ready && sourceBuffer && sourceBuffer.buffered.length > 0) {
            const end = sourceBuffer.buffered.end(sourceBuffer.buffered.length - 1)
            const lag = end - video.currentTime

            if (lag > SEEK_SECONDS) {
                video.currentTime = end - TARGET_SECONDS
                video.playbackRate = 1
            } else if (lag > TARGET_SECONDS + 1.2) {
                video.playbackRate = 1.1
            } else if (lag > TARGET_SECONDS + 0.7) {
                video.playbackRate = 1.04
            } else if (lag < 0.2) {
                video.playbackRate = 0.92
            } else {
                video.playbackRate = 1
            }

            if (video.paused && !video.ended && document.visibilityState === 'visible') {
                video.play().catch(() => {})
            }

            onStats?.({ lag, queued: queue.length, received, rate: video.playbackRate })
        }

        if (ready && Date.now() - lastData > STALL_TIMEOUT && socket?.readyState === WebSocket.OPEN) {
            // 연결은 있는데 조각이 안 온다. 다시 잇는다.
            socket.close()
        }
    }

    firstDataTimer = setTimeout(() => {
        if (!ready) fail('timeout')
    }, FIRST_DATA_TIMEOUT)

    loopTimer = setInterval(regulate, 250)
    connect()

    return { stop }
}
