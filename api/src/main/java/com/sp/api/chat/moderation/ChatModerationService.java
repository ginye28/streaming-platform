package com.sp.api.chat.moderation;

import com.sp.api.chat.dto.BannedWordResponse;
import com.sp.api.chat.dto.ModeratorResponse;
import com.sp.api.chat.dto.RestrictRequest;
import com.sp.api.chat.dto.RestrictionResponse;
import com.sp.api.chat.entity.ChatMessage;
import com.sp.api.chat.event.ChatEvent;
import com.sp.api.chat.event.ChatEventPublisher;
import com.sp.api.chat.repository.ChatMessageRepository;
import com.sp.api.chat.service.ChatMessageAssembler;
import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.ConflictException;
import com.sp.api.common.exception.ForbiddenException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.live.entity.Audience;
import com.sp.api.live.entity.LiveStream;
import com.sp.api.live.repository.LiveStreamRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 채팅 운영. 채널 주인과 매니저가 방송 중에 하는 일(메시지 삭제·고정, 슬로우 모드, 사람 제한)과,
 * 채널 주인이 미리 정해 두는 일(매니저, 금칙어)을 맡는다.
 *
 * 방송 안에서 바뀐 것은 채팅방(WebSocket)으로도 알려, 보고 있는 사람의 화면이 바로 맞춰지게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatModerationService {

    /** 슬로우 모드로 고를 수 있는 대기 시간(초). 0 은 끔. */
    public static final Set<Integer> SLOW_MODE_CHOICES = Set.of(0, 3, 5, 10, 30, 60);

    static final int MAX_MODERATORS = 10;
    static final int MAX_BANNED_WORDS = 100;

    /** 일시 정지로 줄 수 있는 가장 긴 시간(분). 이보다 길게 막으려면 강퇴를 쓴다. */
    static final int MAX_RESTRICT_MINUTES = 7 * 24 * 60;

    private final LiveStreamRepository liveStreamRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChannelModeratorRepository moderatorRepository;
    private final ChatRestrictionRepository restrictionRepository;
    private final ChannelBannedWordRepository bannedWordRepository;
    private final ChatRoleService roleService;
    private final ChatMessageAssembler assembler;
    private final ChatEventPublisher eventPublisher;

    // ---- 방송 안에서 ----

    /** 메시지를 지운다. 쓴 사람 본인이나 주인·매니저가 할 수 있다. */
    @Transactional
    public void deleteMessage(Long liveId, Long messageId, String email) {

        LiveStream live = findLive(liveId);
        User actor = findUser(email);

        ChatMessage message = chatMessageRepository.findWithUserById(messageId)
                .filter(found -> found.getLiveStream().getId().equals(liveId))
                .orElseThrow(() -> new NotFoundException("메시지를 찾을 수 없습니다."));

        boolean author = message.getUser().getId().equals(actor.getId());

        if (!author) {
            requireModerator(live, actor);
        }

        message.delete();

        if (messageId.equals(live.getPinnedMessageId())) {
            live.unpin();
            eventPublisher.event(liveId, ChatEvent.pinned(null));
        }

        eventPublisher.event(liveId, ChatEvent.deleted(messageId));
    }

    /** 메시지를 채팅창 위에 고정한다. 한 번에 하나만 고정되고, 새로 고정하면 이전 것은 풀린다. */
    @Transactional
    public void pin(Long liveId, Long messageId, String email) {

        LiveStream live = findLive(liveId);

        requireModerator(live, findUser(email));

        if (!live.isLive()) {
            throw new BadRequestException("방송 중에만 고정할 수 있습니다.");
        }

        ChatMessage message = chatMessageRepository.findWithUserById(messageId)
                .filter(found -> found.getLiveStream().getId().equals(liveId))
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new NotFoundException("메시지를 찾을 수 없습니다."));

        live.pin(messageId);

        eventPublisher.event(liveId, ChatEvent.pinned(assembler.assembleOne(live, message)));
    }

    @Transactional
    public void unpin(Long liveId, String email) {

        LiveStream live = findLive(liveId);

        requireModerator(live, findUser(email));

        live.unpin();

        eventPublisher.event(liveId, ChatEvent.pinned(null));
    }

    @Transactional
    public void setSlowMode(Long liveId, int seconds, String email) {

        LiveStream live = findLive(liveId);

        requireModerator(live, findUser(email));

        if (!SLOW_MODE_CHOICES.contains(seconds)) {
            throw new BadRequestException("슬로우 모드는 " + SLOW_MODE_CHOICES.stream().sorted().toList() + "초 중에서 고를 수 있습니다.");
        }

        live.changeSlowMode(seconds);

        eventPublisher.event(liveId, ChatEvent.settings(live.getSlowModeSeconds(), live.getChatAudience().name()));
    }

    /** 누가 채팅할 수 있는지를 방송 중에 바꾼다. 수익과 이어지는 설정이라 주인만 바꿀 수 있다. */
    @Transactional
    public void setChatAudience(Long liveId, Audience chatAudience, String email) {

        LiveStream live = findLive(liveId);

        if (!live.isOwnedBy(findUser(email).getId())) {
            throw new ForbiddenException("채널 주인만 바꿀 수 있습니다.");
        }

        live.changeChatAudience(chatAudience);

        eventPublisher.event(liveId, ChatEvent.settings(live.getSlowModeSeconds(), live.getChatAudience().name()));
    }

    /**
     * 사람의 채팅을 막는다. 제한은 방송이 아니라 채널에 걸려서, 다음 방송에도 이어진다.
     * 주인은 제한할 수 없고, 매니저는 주인만 제한할 수 있다.
     */
    @Transactional
    public RestrictionResponse restrict(Long liveId, RestrictRequest request, String email) {

        LiveStream live = findLive(liveId);
        User actor = findUser(email);

        ChatRole actorRole = requireModerator(live, actor);

        Long channelId = live.getUser().getId();

        User target = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        if (target.getId().equals(actor.getId())) {
            throw new BadRequestException("자기 자신은 제한할 수 없습니다.");
        }

        ChatRole targetRole = roleService.roleOf(channelId, target.getId());

        if (targetRole == ChatRole.OWNER) {
            throw new ForbiddenException("채널 주인은 제한할 수 없습니다.");
        }

        if (targetRole == ChatRole.MANAGER && actorRole != ChatRole.OWNER) {
            throw new ForbiddenException("매니저는 주인만 제한할 수 있습니다.");
        }

        LocalDateTime until = request.minutes() == null || request.minutes() <= 0
                ? null
                : LocalDateTime.now().plusMinutes(Math.min(request.minutes(), MAX_RESTRICT_MINUTES));

        ChatRestriction restriction = restrictionRepository.findByChannelIdAndUserId(channelId, target.getId())
                .map(existing -> {
                    existing.restrict(until, request.reason(), actor.getId());
                    return existing;
                })
                .orElseGet(() -> restrictionRepository.save(
                        new ChatRestriction(live.getUser(), target, until, request.reason(), actor.getId())));

        if (Boolean.TRUE.equals(request.purge())) {
            purge(live, target.getId());
        }

        return RestrictionResponse.from(restriction);
    }

    /** 제한을 푼다. 주인과 매니저가 할 수 있다. */
    @Transactional
    public void lift(Long liveId, Long userId, String email) {

        LiveStream live = findLive(liveId);

        requireModerator(live, findUser(email));

        deleteRestriction(live.getUser().getId(), userId);
    }

    // ---- 채널 주인이 미리 정해 두는 것 ----

    public List<ModeratorResponse> moderators(String ownerEmail) {
        return moderatorRepository.findByChannelIdOrderByIdDesc(findUser(ownerEmail).getId()).stream()
                .map(ModeratorResponse::from)
                .toList();
    }

    @Transactional
    public ModeratorResponse addModerator(String ownerEmail, Long userId) {

        User owner = findUser(ownerEmail);

        if (owner.getId().equals(userId)) {
            throw new BadRequestException("채널 주인은 이미 모든 권한이 있습니다.");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        // 이미 매니저면 그대로 돌려준다. 같은 요청을 두 번 눌러도 오류가 나지 않게 한다.
        ChannelModerator existing = moderatorRepository.findByChannelIdAndUserId(owner.getId(), userId).orElse(null);

        if (existing != null) {
            return ModeratorResponse.from(existing);
        }

        if (moderatorRepository.countByChannelId(owner.getId()) >= MAX_MODERATORS) {
            throw new BadRequestException("매니저는 최대 " + MAX_MODERATORS + "명까지 둘 수 있습니다.");
        }

        // 매니저가 된 사람에게 걸려 있던 제한은 의미가 없으니 함께 푼다.
        restrictionRepository.findByChannelIdAndUserId(owner.getId(), userId)
                .ifPresent(restrictionRepository::delete);

        return ModeratorResponse.from(moderatorRepository.save(new ChannelModerator(owner, target)));
    }

    @Transactional
    public void removeModerator(String ownerEmail, Long userId) {

        Long ownerId = findUser(ownerEmail).getId();

        ChannelModerator moderator = moderatorRepository.findByChannelIdAndUserId(ownerId, userId)
                .orElseThrow(() -> new NotFoundException("매니저가 아닙니다."));

        moderatorRepository.delete(moderator);
    }

    public List<BannedWordResponse> bannedWords(String ownerEmail) {
        return bannedWordRepository.findByChannelIdOrderByIdDesc(findUser(ownerEmail).getId()).stream()
                .map(BannedWordResponse::from)
                .toList();
    }

    @Transactional
    public BannedWordResponse addBannedWord(String ownerEmail, String word) {

        User owner = findUser(ownerEmail);

        String normalized = ChatPolicy.normalize(word);

        if (normalized.isEmpty()) {
            throw new BadRequestException("단어를 입력해 주세요.");
        }

        if (bannedWordRepository.existsByChannelIdAndWord(owner.getId(), normalized)) {
            throw new ConflictException("이미 등록된 단어입니다.");
        }

        if (bannedWordRepository.countByChannelId(owner.getId()) >= MAX_BANNED_WORDS) {
            throw new BadRequestException("금칙어는 최대 " + MAX_BANNED_WORDS + "개까지 등록할 수 있습니다.");
        }

        return BannedWordResponse.from(bannedWordRepository.save(new ChannelBannedWord(owner, normalized)));
    }

    @Transactional
    public void removeBannedWord(String ownerEmail, Long wordId) {

        Long ownerId = findUser(ownerEmail).getId();

        bannedWordRepository.delete(bannedWordRepository.findByIdAndChannelId(wordId, ownerId)
                .orElseThrow(() -> new NotFoundException("금칙어를 찾을 수 없습니다.")));
    }

    /** 지금 채팅이 막혀 있는 사람들. */
    public List<RestrictionResponse> restrictions(String ownerEmail) {
        return restrictionRepository.findActive(findUser(ownerEmail).getId(), LocalDateTime.now()).stream()
                .map(RestrictionResponse::from)
                .toList();
    }

    @Transactional
    public void liftByOwner(String ownerEmail, Long userId) {
        deleteRestriction(findUser(ownerEmail).getId(), userId);
    }

    // ---- 도우미 ----

    private void purge(LiveStream live, Long userId) {

        chatMessageRepository.findByLiveStreamIdAndUserId(live.getId(), userId).forEach(ChatMessage::delete);

        // 고정된 메시지가 이 사람 것이면 고정도 푼다.
        if (live.getPinnedMessageId() != null
                && chatMessageRepository.findWithUserById(live.getPinnedMessageId())
                .filter(pinned -> pinned.getUser().getId().equals(userId))
                .isPresent()) {

            live.unpin();
            eventPublisher.event(live.getId(), ChatEvent.pinned(null));
        }

        eventPublisher.event(live.getId(), ChatEvent.purged(userId));
    }

    private void deleteRestriction(Long channelId, Long userId) {

        ChatRestriction restriction = restrictionRepository.findByChannelIdAndUserId(channelId, userId)
                .orElseThrow(() -> new NotFoundException("제한된 사용자가 아닙니다."));

        restrictionRepository.delete(restriction);
    }

    /** 주인이나 매니저가 아니면 거절한다. 맞으면 그 역할을 돌려준다. */
    private ChatRole requireModerator(LiveStream live, User actor) {

        ChatRole role = roleService.roleOf(live.getUser().getId(), actor.getId());

        if (role == null) {
            throw new ForbiddenException("채널 주인이나 매니저만 할 수 있습니다.");
        }

        return role;
    }

    private LiveStream findLive(Long liveId) {
        return liveStreamRepository.findWithUserById(liveId)
                .orElseThrow(() -> new NotFoundException("방송을 찾을 수 없습니다."));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }
}
