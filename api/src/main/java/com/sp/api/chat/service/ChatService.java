package com.sp.api.chat.service;

import com.sp.api.chat.dto.ChatMessageResponse;
import com.sp.api.chat.dto.ChatReplayResponse;
import com.sp.api.chat.entity.ChatMessage;
import com.sp.api.chat.event.ChatEventPublisher;
import com.sp.api.chat.moderation.ChatPolicy;
import com.sp.api.chat.repository.ChatMessageRepository;
import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.ForbiddenException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.live.access.LiveAccessService;
import com.sp.api.live.entity.LiveStream;
import com.sp.api.live.repository.LiveStreamRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    /** 다시보기 한 번에 내려 주는 최대 개수. */
    private static final int REPLAY_MAX_SIZE = 500;

    private final ChatMessageRepository chatMessageRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final UserRepository userRepository;
    private final ChatPolicy chatPolicy;
    private final ChatMessageAssembler assembler;
    private final ChatEventPublisher eventPublisher;
    private final LiveAccessService accessService;

    @Transactional
    public ChatMessageResponse send(Long liveId, String email, String content) {

        LiveStream live = liveStreamRepository.findWithUserById(liveId)
                .orElseThrow(() -> new NotFoundException("방송을 찾을 수 없습니다."));

        if (!live.isLive()) {
            throw new BadRequestException("종료된 방송에는 채팅할 수 없습니다.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        chatPolicy.check(live, user, content, true);

        ChatMessage saved = chatMessageRepository.save(new ChatMessage(live, user, content));

        ChatMessageResponse response = assembler.assembleOne(live, saved);

        eventPublisher.message(liveId, response);

        return response;
    }

    /**
     * 지난 채팅 내역. 최신순으로 내려준다. 지운 메시지는 내용 없이 deleted 표시만 나간다.
     * 영상을 볼 수 없는 방송(멤버십 전용)의 채팅도 볼 수 없다.
     */
    public PageResponse<ChatMessageResponse> findHistory(Long liveId, Pageable pageable, String viewerEmail) {

        LiveStream live = readableLive(liveId, viewerEmail);

        var page = chatMessageRepository.findByLiveStreamIdOrderByIdDesc(liveId, pageable);

        List<ChatMessageResponse> content = assembler.assemble(live, page.getContent(), false);

        return PageResponse.of(page, content);
    }

    /**
     * 다시보기 채팅. 방송이 끝난 뒤에만 읽을 수 있고, 지운 메시지는 빠진다.
     * afterId 다음부터 size 개를 오래된 순으로 주고, 더 있으면 nextAfterId 로 이어 받게 한다.
     */
    public ChatReplayResponse findReplay(Long liveId, Long afterId, int size, String viewerEmail) {

        LiveStream live = readableLive(liveId, viewerEmail);

        if (live.isLive()) {
            throw new BadRequestException("방송이 끝난 뒤에 다시보기 채팅을 볼 수 있습니다.");
        }

        int limit = Math.min(Math.max(size, 1), REPLAY_MAX_SIZE);

        List<ChatMessage> messages = chatMessageRepository.findReplay(
                liveId, afterId == null ? 0L : afterId, PageRequest.of(0, limit));

        List<ChatMessageResponse> content = assembler.assemble(live, messages, true);

        Long next = messages.size() == limit ? messages.get(messages.size() - 1).getId() : null;

        return new ChatReplayResponse(content, next);
    }

    private LiveStream readableLive(Long liveId, String viewerEmail) {

        LiveStream live = liveStreamRepository.findWithUserById(liveId)
                .orElseThrow(() -> new NotFoundException("방송을 찾을 수 없습니다."));

        if (!accessService.canWatch(live, viewerEmail)) {
            throw new ForbiddenException("이 방송의 채팅은 구독자만 볼 수 있습니다.");
        }

        return live;
    }
}
