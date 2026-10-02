package com.sp.api.chat.moderation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** 채널 안에서 한 사람이 가진 채팅 운영 역할(주인·매니저·없음)을 가린다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoleService {

    private final ChannelModeratorRepository moderatorRepository;

    /** 주인이거나 매니저면 그 역할, 아니면 null. */
    public ChatRole roleOf(Long channelId, Long userId) {

        if (channelId.equals(userId)) {
            return ChatRole.OWNER;
        }

        return moderatorRepository.existsByChannelIdAndUserId(channelId, userId) ? ChatRole.MANAGER : null;
    }

    /** 채팅 한 페이지의 작성자 역할을 한 번에 가린다. 역할이 없는 사람은 맵에 없다. */
    public Map<Long, ChatRole> rolesAmong(Long channelId, Set<Long> userIds) {

        Map<Long, ChatRole> roles = new HashMap<>();

        if (userIds.isEmpty()) {
            return roles;
        }

        moderatorRepository.findModeratorIdsAmong(channelId, userIds)
                .forEach(id -> roles.put(id, ChatRole.MANAGER));

        // 주인은 매니저 표에 없어도 주인이다.
        if (userIds.contains(channelId)) {
            roles.put(channelId, ChatRole.OWNER);
        }

        return roles;
    }
}
