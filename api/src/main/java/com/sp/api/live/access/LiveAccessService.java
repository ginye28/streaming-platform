package com.sp.api.live.access;

import com.sp.api.live.entity.Audience;
import com.sp.api.live.entity.LiveStream;
import com.sp.api.live.repository.LiveStreamRepository;
import com.sp.api.subscribe.entity.SubscriptionTier;
import com.sp.api.subscribe.repository.ChannelTier;
import com.sp.api.subscribe.repository.SubscribeRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 방송을 볼 수 있는지, 채팅을 쓸 수 있는지를 구독 등급으로 가린다.
 *
 * 채널 주인은 언제나 통과한다. 유료 구독은 기간이 끝났으면 일반 구독으로 본다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveAccessService {

    private final UserRepository userRepository;
    private final SubscribeRepository subscribeRepository;
    private final LiveStreamRepository liveStreamRepository;

    /** 이 방송의 영상을 이 시청자가 볼 수 있는지. 비로그인이면 viewerEmail 은 null. */
    public boolean canWatch(LiveStream live, String viewerEmail) {
        return canUse(live.getAudience(), live.getUser().getId(), viewerEmail);
    }

    /** 방송 id 로 묻는 판. 없는 방송이면 막을 이유가 없으니 true. WebSocket 구독을 걸러낼 때 쓴다. */
    public boolean canWatchLive(Long liveId, String viewerEmail) {
        return liveStreamRepository.findWithUserById(liveId)
                .map(live -> canWatch(live, viewerEmail))
                .orElse(true);
    }

    public boolean canUse(Audience audience, Long channelId, String viewerEmail) {

        if (!audience.isRestricted()) {
            return true;
        }

        if (viewerEmail == null) {
            return false;
        }

        return userRepository.findByEmail(viewerEmail)
                .map(viewer -> canUse(audience, channelId, viewer))
                .orElse(false);
    }

    public boolean canUse(Audience audience, Long channelId, User viewer) {

        if (!audience.isRestricted() || viewer.getId().equals(channelId)) {
            return true;
        }

        return audience.allows(tierOf(viewer.getId(), channelId));
    }

    /** 이 시청자의 이 채널 구독 등급. 구독하지 않았으면 null. */
    public SubscriptionTier tierOf(Long viewerId, Long channelId) {

        LocalDateTime now = LocalDateTime.now();

        return subscribeRepository.findBySubscriberIdAndChannelId(viewerId, channelId)
                .map(subscribe -> subscribe.effectiveTier(now))
                .orElse(null);
    }

    /**
     * 방송 목록에서 이 시청자가 영상을 볼 수 있는 방송의 id.
     * 방송마다 묻지 않고 구독 등급을 한 번에 읽는다.
     */
    public Set<Long> watchableLiveIds(Collection<LiveStream> lives, String viewerEmail) {

        Set<Long> watchable = new HashSet<>();
        Set<Long> restrictedChannels = new HashSet<>();

        for (LiveStream live : lives) {
            if (live.getAudience().isRestricted()) {
                restrictedChannels.add(live.getUser().getId());
            } else {
                watchable.add(live.getId());
            }
        }

        if (restrictedChannels.isEmpty() || viewerEmail == null) {
            return watchable;
        }

        User viewer = userRepository.findByEmail(viewerEmail).orElse(null);

        if (viewer == null) {
            return watchable;
        }

        LocalDateTime now = LocalDateTime.now();

        Map<Long, SubscriptionTier> tiers = subscribeRepository
                .findTiersOfSubscriber(viewer.getId(), restrictedChannels)
                .stream()
                .collect(Collectors.toMap(ChannelTier::channelId, tier -> tier.effectiveTier(now)));

        for (LiveStream live : lives) {

            if (!live.getAudience().isRestricted()) {
                continue;
            }

            Long channelId = live.getUser().getId();

            if (channelId.equals(viewer.getId()) || live.getAudience().allows(tiers.get(channelId))) {
                watchable.add(live.getId());
            }
        }

        return watchable;
    }
}
