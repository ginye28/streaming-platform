import { API_BASE_URL, freshAccessToken } from './api.js'

/** 브라우저 방송 업로드 주소. API 의 /ingest. */
const INGEST_URL = `${API_BASE_URL.replace(/^http/, 'ws')}/ingest`

/** 서버(ffmpeg)가 영상을 다시 인코딩하지 않고 포장만 바꾸므로 H.264 로 보내야 한다. */
const MIME_CANDIDATES = [
    'video/webm;codecs=h264,opus',
    'video/webm;codecs=avc1,opus',
    'video/webm;codecs=h264',
    'video/x-matroska;codecs=avc1,opus',
    'video/x-matroska;codecs=avc1',
]

const VIDEO_BITS_PER_SECOND = 2_500_000
const AUDIO_BITS_PER_SECOND = 128_000

/** 서버로 못 나간 데이터가 이만큼 쌓이면 네트워크가 못 따라가는 것이므로 멈춘다. */
const MAX_BUFFERED_BYTES = 8 * 1024 * 1024

/** 이 브라우저가 H.264 로 녹화할 수 있으면 그 형식, 아니면 null. (Chrome·Edge 는 되고 Firefox·Safari 는 안 된다) */
export function pickMimeType() {
    if (typeof MediaRecorder === 'undefined') return null

    return MIME_CANDIDATES.find((type) => MediaRecorder.isTypeSupported(type)) ?? null
}

/** 화면 공유(소리 포함) 또는 카메라를 연다. 사용자가 누른 직후에 불러야 브라우저가 허용한다. */
export async function openCapture({ source, withMic }) {
    if (source === 'screen') {
        const screen = await navigator.mediaDevices.getDisplayMedia({
            video: { frameRate: 30, width: { max: 1920 }, height: { max: 1080 } },
            audio: true,
        })

        // 화면 공유는 소리가 없을 때가 많다. 원하면 마이크를 덧붙인다.
        if (withMic && screen.getAudioTracks().length === 0) {
            try {
                const mic = await navigator.mediaDevices.getUserMedia({ audio: true })
                mic.getAudioTracks().forEach((track) => screen.addTrack(track))
            } catch {
                // 마이크를 못 열어도 화면만으로 방송한다
            }
        }

        return screen
    }

    return navigator.mediaDevices.getUserMedia({
        video: { width: { ideal: 1280 }, height: { ideal: 720 }, frameRate: { ideal: 30 } },
        audio: withMic,
    })
}

/**
 * 잡은 화면/카메라(stream)를 서버로 보내 방송을 시작한다.
 *
 * 1. /ingest 에 연결해 로그인 토큰을 보낸다.
 * 2. 서버가 방송을 열면 MediaRecorder 로 1초마다 조각을 보낸다.
 * 3. 끊기면(어느 쪽이든) 방송이 끝난다.
 *
 * @returns {{ stop: () => void }}
 */
export async function startBroadcast(stream, { onStarted, onClosed, onError, onStats }) {
    const mimeType = pickMimeType()

    if (!mimeType) {
        throw new Error('이 브라우저는 영상을 H.264 로 보낼 수 없어요. Chrome 또는 Edge 에서 열어 주세요.')
    }

    const token = await freshAccessToken()
    const socket = new WebSocket(INGEST_URL)
    socket.binaryType = 'arraybuffer'

    let recorder = null
    let sentBytes = 0
    let startedAt = 0
    let statsTimer = null
    let finished = false

    function finish(reason) {
        if (finished) return
        finished = true

        clearInterval(statsTimer)

        if (recorder && recorder.state !== 'inactive') {
            try {
                recorder.stop()
            } catch {
                // 이미 멈춘 것
            }
        }

        if (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING) {
            socket.close()
        }

        onClosed(reason)
    }

    socket.onopen = () => {
        socket.send(JSON.stringify({ type: 'start', token }))
    }

    socket.onmessage = (event) => {
        const message = JSON.parse(event.data)

        if (message.type === 'error') {
            onError(message.message)
            finish('error')
            return
        }

        if (message.type !== 'started') return

        recorder = new MediaRecorder(stream, {
            mimeType,
            videoBitsPerSecond: VIDEO_BITS_PER_SECOND,
            audioBitsPerSecond: AUDIO_BITS_PER_SECOND,
        })

        recorder.ondataavailable = (chunk) => {
            if (chunk.data.size === 0 || socket.readyState !== WebSocket.OPEN) return

            if (socket.bufferedAmount > MAX_BUFFERED_BYTES) {
                onError('네트워크가 느려서 방송을 이어 가지 못했어요.')
                finish('slow')
                return
            }

            socket.send(chunk.data)
            sentBytes += chunk.data.size
        }

        recorder.onerror = () => {
            onError('녹화 중 오류가 나서 방송을 멈췄어요.')
            finish('error')
        }

        startedAt = Date.now()
        statsTimer = setInterval(() => {
            const seconds = Math.max(1, (Date.now() - startedAt) / 1000)

            onStats({ seconds, sentBytes, kbps: Math.round((sentBytes * 8) / seconds / 1000) })
        }, 1000)

        recorder.start(1000)
        onStarted(message.liveId)
    }

    socket.onerror = () => {
        onError('방송 서버에 연결하지 못했어요.')
    }

    socket.onclose = () => finish('closed')

    // 화면 공유를 브라우저의 "공유 중지"로 끝내면 방송도 끝낸다.
    stream.getVideoTracks().forEach((track) => track.addEventListener('ended', () => finish('ended')))

    return { stop: () => finish('stopped') }
}
