package com.sp.api.chat.service;

import com.sp.api.chat.dto.ChatMessageResponse;
import com.sp.api.chat.entity.ChatMessage;
import com.sp.api.chat.moderation.ChatRole;
import com.sp.api.chat.moderation.ChatRoleService;
import com.sp.api.live.entity.LiveStream;
import com.sp.api.vtuber.dto.OshiMark;
import com.sp.api.vtuber.service.ChannelProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 채팅 메시지를 화면에 내려 줄 모양으로 바꾼다. 오시마크와 채널 역할은 한 페이지를 한 번에 가린다
 * (줄마다 물어보면 N+1 이 된다).
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageAssembler {

    private final ChannelProfileService channelProfileService;
    private final ChatRoleService roleService;

    /** @param replay true 면 방송 시작부터 몇 초째인지(offsetSeconds)를 함께 채운다 */
    public List<ChatMessageResponse> assemble(LiveStream live, List<ChatMessage> messages, boolean replay) {

        Long channelId = live.getUser().getId();

        Set<Long> authorIds = messages.stream()
                .map(message -> message.getUser().getId())
                .collect(Collectors.toSet());

        Map<Long, OshiMark> marks = channelProfileService.oshiMarksFor(channelId, authorIds);
        Map<Long, ChatRole> roles = roleService.rolesAmong(channelId, authorIds);

        return messages.stream()
                .map(message -> ChatMessageResponse.from(
                        message,
                        marks.get(message.getUser().getId()),
                        roles.get(message.getUser().getId()),
                        replay ? offsetSeconds(live, message) : null))
                .toList();
    }

    public ChatMessageResponse assembleOne(LiveStream live, ChatMessage message) {
        return assemble(live, List.of(message), false).get(0);
    }

    private static Long offsetSeconds(LiveStream live, ChatMessage message) {
        return Math.max(0, Duration.between(live.getStartedAt(), message.getCreatedAt()).toSeconds());
    }
}
