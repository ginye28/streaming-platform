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

/**
 * 화질 세 가지. 서버는 영상을 다시 인코딩하지 않고 그대로 내보내서, 시청자 한 명이 받는 양이 곧 보내는 비트레이트다.
 *  - 최고: 1080p · 4Mbps. 글자가 번지지 않는다. 서버(무료 0.1 CPU)와 시청자 망이 버티려면 시청자가 몇 명 안 될 때만 쓴다.
 *  - 높음: 1080p · 2.5Mbps. 시청자가 적을 때 글자·영상이 또렷하다. 13명이 보면 서버에서 최대 약 33Mbps 가 나간다.
 *  - 보통: 720p · 1.5Mbps. 학교 와이파이처럼 흔들리는 망이나 시청자가 10명 안팎 이상일 때 끊김이 적다(최대 약 20Mbps).
 */
export const QUALITIES = {
    best: { label: '최고', detail: '1080p · 4Mbps — 가장 또렷해요. 시청자가 한두 명일 때', width: 1920, height: 1080, bitsPerSecond: 4_000_000 },
    high: { label: '높음', detail: '1080p · 2.5Mbps — 또렷해요. 시청자가 적을 때', width: 1920, height: 1080, bitsPerSecond: 2_500_000 },
    normal: { label: '보통', detail: '720p · 1.5Mbps — 시청자가 많거나 망이 느릴 때(수업 등)', width: 1280, height: 720, bitsPerSecond: 1_500_000 },
}

const AUDIO_BITS_PER_SECOND = 128_000

/** 서버로 못 나간 데이터가 이만큼 쌓이면 네트워크가 못 따라가는 것이므로 멈춘다. */
const MAX_BUFFERED_BYTES = 8 * 1024 * 1024

/** 이 브라우저가 H.264 로 녹화할 수 있으면 그 형식, 아니면 null. (Chrome·Edge 는 되고 Firefox·Safari 는 안 된다) */
export function pickMimeType() {
    if (typeof MediaRecorder === 'undefined') return null

    return MIME_CANDIDATES.find((type) => MediaRecorder.isTypeSupported(type)) ?? null
}

/**
 * 화면 공유(소리 포함) 또는 카메라를 연다. 사용자가 누른 직후에 불러야 브라우저가 허용한다.
 *
 * @returns {{ stream: MediaStream, audio: 'screen' | 'mic' | 'none', micError: string | null }}
 *   audio 는 실제로 소리가 들어갔는지. 마이크를 원했는데 못 열었으면 micError 에 이유가 담긴다.
 */
export async function openCapture({ source, withMic, quality = 'high' }) {
    let micError = null
    const { width, height } = QUALITIES[quality] ?? QUALITIES.high

    if (source === 'screen') {
        const screen = await navigator.mediaDevices.getDisplayMedia({
            video: { frameRate: 30, width: { max: width }, height: { max: height } },
            audio: true,
        })

        let audio = screen.getAudioTracks().length > 0 ? 'screen' : 'none'

        // 화면 공유는 소리가 없을 때가 많다. 원하면 마이크를 덧붙인다.
        if (withMic && audio === 'none') {
            try {
                const mic = await navigator.mediaDevices.getUserMedia({ audio: true })
                mic.getAudioTracks().forEach((track) => screen.addTrack(track))
                audio = 'mic'
            } catch (e) {
                micError = e?.name === 'NotAllowedError' ? 'denied' : 'unavailable'
            }
        }

        return { stream: screen, audio, micError }
    }

    try {
        const camera = await navigator.mediaDevices.getUserMedia({
            video: { width: { ideal: width }, height: { ideal: height }, frameRate: { ideal: 30 } },
            audio: withMic,
        })

        return { stream: camera, audio: camera.getAudioTracks().length > 0 ? 'mic' : 'none', micError }
    } catch (e) {
        if (!withMic) throw e

        // 마이크가 막혀 있어도 카메라 영상만으로 방송할 수 있게 한 번 더 시도한다.
        const camera = await navigator.mediaDevices.getUserMedia({
            video: { width: { ideal: width }, height: { ideal: height }, frameRate: { ideal: 30 } },
        })

        return {
            stream: camera,
            audio: 'none',
            micError: e?.name === 'NotAllowedError' ? 'denied' : 'unavailable',
        }
    }
}

/**
 * 잡은 화면/카메라(stream)를 서버로 보내 방송을 시작한다.
 *
 * 1. /ingest 에 연결해 로그인 토큰을 보낸다.
 * 2. 서버가 방송을 열면 MediaRecorder 로 0.25초마다 조각을 보낸다.
 * 3. 끊기면(어느 쪽이든) 방송이 끝난다.
 *
 * @returns {{ stop: () => void }}
 */
export async function startBroadcast(stream, { onStarted, onClosed, onError, onStats, quality = 'high' }) {
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
            videoBitsPerSecond: (QUALITIES[quality] ?? QUALITIES.high).bitsPerSecond,
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

        // 0.25초마다 보낸다. 1초로 모으면 시청 지연이 그만큼 늘어난다.
        recorder.start(250)
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
