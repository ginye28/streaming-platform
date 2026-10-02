package com.sp.api.chat.config;

import com.sp.api.common.jwt.JwtProvider;
import com.sp.api.live.access.LiveAccessService;
import com.sp.api.user.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP CONNECT 프레임의 Authorization 헤더로 사용자를 식별한다.
 * WebSocket 은 HTTP 필터 체인을 타지 않으므로 인증을 따로 걸어야 한다.
 *
 * 구독도 여기서 거른다. 멤버십 전용 방송의 채팅방은 볼 수 있는 사람만 구독할 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    /** /topic/lives/{id}, /topic/lives/{id}/events 처럼 채팅 내용이 흐르는 길. 시청자 수(/viewers)는 뺀다. */
    private static final Pattern CHAT_TOPIC = Pattern.compile("^/topic/lives/(\\d+)(/events)?$");

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final LiveAccessService liveAccessService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            checkSubscription(accessor);
            return message;
        }

        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = resolveToken(accessor.getFirstNativeHeader(HEADER));

        if (token == null) {
            // 비로그인도 연결은 허용한다. 시청만 하고 채팅은 못 보내는 상태가 된다.
            return message;
        }

        String email = jwtProvider.parseEmail(token);

        if (email == null) {
            return message;
        }

        try {
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()));

        } catch (UsernameNotFoundException e) {
            log.debug("WebSocket 토큰의 사용자를 찾을 수 없음");
        }

        return message;
    }

    private void checkSubscription(StompHeaderAccessor accessor) {

        String destination = accessor.getDestination();

        if (destination == null) {
            return;
        }

        Matcher matcher = CHAT_TOPIC.matcher(destination);

        if (!matcher.matches()) {
            return;
        }

        String email = accessor.getUser() == null ? null : accessor.getUser().getName();

        if (!liveAccessService.canWatchLive(Long.valueOf(matcher.group(1)), email)) {
            throw new MessagingException("이 방송은 구독자만 볼 수 있습니다.");
        }
    }

    private String resolveToken(String header) {
        return header != null && header.startsWith(PREFIX) ? header.substring(PREFIX.length()) : null;
    }
}
