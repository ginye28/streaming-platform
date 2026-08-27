package com.sp.api.chat.service;

import com.sp.api.chat.dto.ChatMessageResponse;
import com.sp.api.chat.entity.ChatMessage;
import com.sp.api.chat.repository.ChatMessageRepository;
import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.live.entity.LiveStream;
import com.sp.api.live.repository.LiveStreamRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import com.sp.api.vtuber.service.ChannelProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final UserRepository userRepository;
    private final ChannelProfileService channelProfileService;

    @Transactional
    public ChatMessageResponse send(Long liveId, String email, String content) {

        LiveStream live = liveStreamRepository.findWithUserById(liveId)
                .orElseThrow(() -> new NotFoundException("방송을 찾을 수 없습니다."));

        if (!live.isLive()) {
            throw new BadRequestException("종료된 방송에는 채팅할 수 없습니다.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        ChatMessage saved = chatMessageRepository.save(new ChatMessage(live, user, content));

        return ChatMessageResponse.from(saved, oshiMarkOf(live, user.getId()));
    }

    /** 지난 채팅 내역. 최신순으로 내려준다. */
    public PageResponse<ChatMessageResponse> findHistory(Long liveId, Pageable pageable) {

        if (!liveStreamRepository.existsById(liveId)) {
            throw new NotFoundException("방송을 찾을 수 없습니다.");
        }

        var page = chatMessageRepository.findByLiveStreamIdOrderByIdDesc(liveId, pageable);

        // 한 페이지의 오시마크를 한 번에 가린다. 줄마다 물어보면 N+1 이 된다.
        Long channelId = liveStreamRepository.findWithUserById(liveId)
                .map(live -> live.getUser().getId())
                .orElse(null);

        Set<Long> authorIds = page.getContent().stream()
                .map(message -> message.getUser().getId())
                .collect(Collectors.toSet());

        Map<Long, String> marks = channelId == null
                ? Map.of()
                : channelProfileService.oshiMarksFor(channelId, authorIds);

        return PageResponse.from(
                page.map(message -> ChatMessageResponse.from(
                        message, marks.get(message.getUser().getId())))
        );
    }

    /** 실시간 한 건. 이미 방송 정보를 들고 있으므로 다시 찾지 않는다. */
    private String oshiMarkOf(LiveStream live, Long authorId) {
        return channelProfileService
                .oshiMarksFor(live.getUser().getId(), Set.of(authorId))
                .get(authorId);
    }
}
