import { assetUrl } from '../assets.js'

/** 맡은 일의 한국어 이름. 서버는 코드로만 보낸다. */
const CREDIT_ROLES = {
    ILLUSTRATOR: '일러스트',
    RIGGER: '리깅',
    MODELER_3D: '3D 모델링',
    LOGO: '로고',
    BGM: 'BGM',
    OTHER: '그 밖',
}

/**
 * 채널 페이지에 붙는 그 사람의 정보.
 * 아직 아무것도 안 채운 채널에서는 아무것도 그리지 않는다.
 */
export default function ChannelIdentity({ profile }) {
    if (!profile) return null

    // 팬네임은 이 위 메타 줄이 이미 쓴다. 여기서 또 쓰면 같은 말이 두 번 나온다.
    const hasAnything =
        profile.oshiMarkUrl ||
        profile.debutOn ||
        profile.graduatedOn ||
        profile.credits.length > 0

    if (!hasAnything) return null

    return (
        <div className="identity">
            <div className="identity__badges">
                {profile.oshiMarkUrl && (
                    <img
                        className="identity__mark"
                        src={assetUrl(profile.oshiMarkUrl)}
                        alt="오시마크"
                        title="구독하면 채팅에 이 표식이 붙습니다"
                    />
                )}

                {profile.daysUntilDebut != null && (
                    <span className="identity__tag identity__tag--debut">
                        데뷔 D-{profile.daysUntilDebut}
                    </span>
                )}

                {profile.graduated && (
                    <span className="identity__tag identity__tag--graduated">졸업</span>
                )}
            </div>

            {(profile.debutOn || profile.graduatedOn) && (
                <p className="meta">
                    {profile.debutOn && `데뷔 ${profile.debutOn}`}
                    {profile.debutOn && profile.graduatedOn && ' · '}
                    {profile.graduatedOn && `졸업 ${profile.graduatedOn}`}
                </p>
            )}

            {profile.credits.length > 0 && (
                <details className="identity__credits">
                    <summary>만들어 주신 분들</summary>

                    <ul>
                        {profile.credits.map((credit, index) => (
                            <li key={index}>
                                <span className="identity__role">
                                    {CREDIT_ROLES[credit.role] ?? credit.role}
                                </span>{' '}
                                {credit.link ? (
                                    <a href={credit.link} target="_blank" rel="noreferrer noopener">
                                        {credit.name}
                                    </a>
                                ) : (
                                    credit.name
                                )}
                            </li>
                        ))}
                    </ul>
                </details>
            )}
        </div>
    )
}

export { CREDIT_ROLES }
