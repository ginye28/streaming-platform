import { getChannel } from '../api.js'
import { assetUrl } from '../assets.js'
import { useAsyncData } from '../useAsyncData.js'
import Link from './Link.jsx'
import SubscribeBar from './SubscribeBar.jsx'

/**
 * 영상·방송 화면처럼 채널 정보를 따로 들고 있지 않은 곳에서 쓰는 구독 막대.
 * 채널을 직접 받아 와서 SubscribeBar 에 넘긴다.
 * onChanged 를 주면 구독이 바뀐 뒤에 불러 준다(구독자 전용 방송처럼 구독 여부로 화면이 달라지는 곳이 다시 읽게).
 * identity 를 주면 채널 얼굴(아바타)과 구독자 수를 구독 막대 곁에 함께 보여 준다.
 */
export default function ChannelSubscribe({ channelId, onChanged, identity = false }) {
    const { data: channel, reload, fail, error } = useAsyncData(
        () => getChannel(channelId),
        [channelId]
    )

    // 실패해도 막대는 그대로 둔다. 오류만 곁에 보여 주고 다시 시도할 수 있게 한다.
    const bar = channel && (
        <SubscribeBar
            channel={channel}
            onChanged={() => {
                reload()
                onChanged?.()
            }}
            onFail={fail}
        />
    )

    return (
        <>
            {error && <p className="error">{error}</p>}

            {identity && channel ? (
                <div className="channel-row">
                    <Link
                        to={{ view: 'channel', id: channel.id }}
                        className="channel-avatar"
                        aria-label={`${channel.nickname} 채널`}
                    >
                        {channel.profileImage ? (
                            <img src={assetUrl(channel.profileImage)} alt="" loading="lazy" decoding="async" />
                        ) : (
                            channel.nickname.slice(0, 1)
                        )}
                    </Link>

                    <div className="channel-row__body">
                        <p className="meta">구독자 {channel.subscriberCount.toLocaleString('ko-KR')}명</p>
                        {bar}
                    </div>
                </div>
            ) : (
                bar
            )}
        </>
    )
}
