package com.sp.api.vtuber.service;

import com.sp.api.common.exception.NotFoundException;
import com.sp.api.subscribe.entity.SubscriptionTier;
import com.sp.api.subscribe.repository.SubscribeRepository;
import com.sp.api.subscribe.repository.SubscriberMark;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import com.sp.api.vtuber.dto.ChannelProfileRequest;
import com.sp.api.vtuber.dto.ChannelProfileResponse;
import com.sp.api.vtuber.dto.OshiMark;
import com.sp.api.vtuber.entity.ChannelProfile;
import com.sp.api.vtuber.entity.ModelCredit;
import com.sp.api.vtuber.repository.ChannelProfileRepository;
import com.sp.api.vtuber.repository.ModelCreditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * 채널의 정체성 — 오시마크 · 팬네임 · 데뷔/졸업 · 모델 크레딧.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChannelProfileService {

    private final ChannelProfileRepository profileRepository;
    private final ModelCreditRepository creditRepository;
    private final SubscribeRepository subscribeRepository;
    private final UserRepository userRepository;

    public ChannelProfileResponse find(Long channelId) {

        User channel = userRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("채널을 찾을 수 없습니다."));

        return ChannelProfileResponse.of(
                channel,
                profileRepository.findByUserId(channelId).orElse(null),
                subscribeRepository.countByChannelId(channelId),
                creditRepository.findByUserId(channelId)
        );
    }

    public ChannelProfileResponse findMine(String email) {
        return find(findUser(email).getId());
    }

    @Transactional
    public ChannelProfileResponse updateMine(String email, ChannelProfileRequest request) {

        User me = findUser(email);

        ChannelProfile profile = profileRepository.findByUserId(me.getId())
                .orElseGet(() -> profileRepository.save(new ChannelProfile(me)));

        profile.update(
                request.getOshiMarkUrl(),
                request.getPaidOshiMarkUrl(),
                request.getFanName(),
                request.getDebutOn(),
                request.getGraduatedOn());

        // 보내지 않았으면 손대지 않는다. 빈 배열을 보내야 지운다.
        if (request.getCredits() != null) {
            replaceCredits(me, request);
        }

        return find(me.getId());
    }

    /** 줄마다 수정·삭제를 따지는 것보다 통째로 갈아 끼우는 편이 단순하다. */
    private void replaceCredits(User me, ChannelProfileRequest request) {

        creditRepository.deleteByUserId(me.getId());

        List<ModelCredit> credits = IntStream.range(0, request.getCredits().size())
                .mapToObj(index -> {
                    var payload = request.getCredits().get(index);
                    return new ModelCredit(
                            me, payload.getRole(), payload.getName(), payload.getLink(), index);
                })
                .toList();

        creditRepository.saveAll(credits);
    }

    /**
     * 이 채널을 구독한 사람들의 이름 옆에 붙일 오시마크.
     *
     * 규칙은 한 곳에서만 정한다. 채팅과 댓글이 같은 함수를 쓰므로 둘이 어긋나지 않는다.
     * - 구독하지 않은 사람에게는 붙지 않는다. 아무나 달 수 있으면 표식이 아니게 된다.
     * - 구독자가 이 채널의 마크를 껐으면(markVisible = false) 붙지 않는다.
     * - 유료 구독자는 유료 마크를 달고, 유료 마크가 없으면 일반 마크를 단다.
     * 한 페이지를 한 번에 처리하려고 작성자 id 를 모아 받는다. 줄마다 물어보면 N+1 이 된다.
     *
     * @return 오시마크를 붙일 사용자 id → 마크. 붙일 사람이 없으면 빈 map.
     */
    public Map<Long, OshiMark> oshiMarksFor(Long channelId, Set<Long> authorIds) {

        if (authorIds.isEmpty()) {
            return Map.of();
        }

        ChannelProfile profile = profileRepository.findByUserId(channelId).orElse(null);

        if (profile == null) {
            return Map.of();
        }

        Map<Long, OshiMark> marks = new HashMap<>();

        for (SubscriberMark subscriber : subscribeRepository.findSubscriberMarksAmong(channelId, authorIds)) {

            if (!subscriber.markVisible()) {
                continue;
            }

            String url = subscriber.tier() == SubscriptionTier.PAID && profile.getPaidOshiMarkUrl() != null
                    ? profile.getPaidOshiMarkUrl()
                    : profile.getOshiMarkUrl();

            if (url != null) {
                marks.put(subscriber.subscriberId(), new OshiMark(url, subscriber.tier()));
            }
        }

        return marks;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }
}
