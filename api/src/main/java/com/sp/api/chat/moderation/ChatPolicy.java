package com.sp.api.chat.moderation;

import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.ForbiddenException;
import com.sp.api.live.access.LiveAccessService;
import com.sp.api.live.entity.Audience;
import com.sp.api.live.entity.LiveStream;
import com.sp.api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 이 사람이 지금 이 방송에 이 글을 쓸 수 있는지 정하는 규칙 한 곳.
 * 채팅 전송과 후원(후원 메시지도 채팅에 올라가므로)이 같은 규칙을 쓴다.
 *
 * 검사 순서: 제한(일시 정지·강퇴) → 채팅 가능 대상 → 금칙어 → 슬로우 모드.
 * 슬로우 모드를 맨 뒤에 두는 이유는 다른 이유로 거절된 글이 대기 시간을 쓰지 않게 하려는 것이다.
 * 채널 주인과 매니저는 전부 건너뛴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatPolicy {

    private final ChatRoleService roleService;
    private final ChatRestrictionRepository restrictionRepository;
    private final ChannelBannedWordRepository bannedWordRepository;
    private final LiveAccessService accessService;
    private final SlowModeLimiter slowModeLimiter;

    /**
     * @param applySlowMode 후원은 슬로우 모드를 적용하지 않는다(돈을 낸 메시지를 대기 시간으로 막지 않는다)
     * @throws ForbiddenException  제한됐거나 채팅할 수 없는 대상일 때
     * @throws BadRequestException 금칙어가 있거나 슬로우 모드에 걸렸을 때
     */
    public void check(LiveStream live, User user, String content, boolean applySlowMode) {

        Long channelId = live.getUser().getId();

        if (roleService.roleOf(channelId, user.getId()) != null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        restrictionRepository.findByChannelIdAndUserId(channelId, user.getId())
                .filter(restriction -> restriction.isActive(now))
                .ifPresent(restriction -> {

                    if (restriction.isPermanent()) {
                        throw new ForbiddenException("이 채널에서 채팅이 차단되었습니다.");
                    }

                    long seconds = Duration.between(now, restriction.getRestrictedUntil()).toSeconds();
                    long minutes = Math.max(1, (seconds + 59) / 60);

                    throw new ForbiddenException("채팅이 일시 정지되었습니다. " + minutes + "분 뒤에 다시 쓸 수 있어요.");
                });

        if (!accessService.canUse(live.getChatAudience(), channelId, user)) {
            throw new ForbiddenException(live.getChatAudience() == Audience.PAID
                    ? "유료 구독자만 채팅할 수 있어요."
                    : "구독자만 채팅할 수 있어요.");
        }

        if (content != null && !content.isBlank() && containsBannedWord(channelId, content)) {
            throw new BadRequestException("사용할 수 없는 단어가 들어 있어요.");
        }

        if (applySlowMode && live.getSlowModeSeconds() > 0) {

            long wait = slowModeLimiter.tryAcquire(live.getId(), user.getId(), live.getSlowModeSeconds());

            if (wait > 0) {
                throw new BadRequestException("슬로우 모드예요. " + wait + "초 뒤에 다시 보낼 수 있어요.");
            }
        }
    }

    private boolean containsBannedWord(Long channelId, String content) {

        String normalized = normalize(content);

        return bannedWordRepository.findByChannelIdOrderByIdDesc(channelId).stream()
                .anyMatch(banned -> normalized.contains(banned.getWord()));
    }

    /** 대소문자와 공백을 무시하고 비교하려고 같은 모양으로 맞춘다. */
    public static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}
