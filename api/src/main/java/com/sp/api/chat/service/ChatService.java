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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final UserRepository userRepository;

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

        return ChatMessageResponse.from(saved);
    }

    /** 지난 채팅 내역. 최신순으로 내려준다. */
    public PageResponse<ChatMessageResponse> findHistory(Long liveId, Pageable pageable) {

        if (!liveStreamRepository.existsById(liveId)) {
            throw new NotFoundException("방송을 찾을 수 없습니다.");
        }

        return PageResponse.from(
                chatMessageRepository.findByLiveStreamIdOrderByIdDesc(liveId, pageable)
                        .map(ChatMessageResponse::from)
        );
    }
}
