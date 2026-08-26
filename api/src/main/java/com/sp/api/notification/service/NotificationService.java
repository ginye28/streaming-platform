package com.sp.api.notification.service;

import com.sp.api.comment.entity.Comment;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.live.entity.LiveStream;
import com.sp.api.notification.dto.NotificationResponse;
import com.sp.api.notification.entity.Notification;
import com.sp.api.notification.repository.NotificationRepository;
import com.sp.api.subscribe.repository.SubscribeRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SubscribeRepository subscribeRepository;
    private final UserRepository userRepository;

    /** 방송이 시작되면 구독자 전원에게 알림을 남긴다. */
    @Transactional
    public int notifyLiveStart(LiveStream live) {

        List<User> subscribers =
                subscribeRepository.findSubscribersOfChannel(live.getUser().getId());

        if (subscribers.isEmpty()) {
            return 0;
        }

        String message = live.getUser().getNickname() + " 님이 방송을 시작했습니다.";

        List<Notification> notifications = subscribers.stream()
                .map(subscriber -> new Notification(
                        subscriber,
                        Notification.Type.LIVE_START,
                        message,
                        live.getUser().getId(),
                        live.getId()
                ))
                .toList();

        notificationRepository.saveAll(notifications);

        return notifications.size();
    }

    /**
     * 영상에 댓글이 달리면 영상 주인에게 알림을 남긴다.
     * 답글은 원 댓글 작성자에게만 가므로 여기서는 다루지 않는다 — 한 번에 두 곳으로 알리지 않는다.
     */
    @Transactional
    public void notifyComment(Comment comment) {
        notify(comment.getStream().getUser(), comment, Notification.Type.STREAM_COMMENT,
                " 님이 회원님의 영상에 댓글을 남겼습니다.");
    }

    /**
     * 답글이 달리면 원 댓글 작성자에게 알림을 남긴다.
     * 자기 댓글에 스스로 단 답글은 알리지 않는다.
     */
    @Transactional
    public void notifyReply(Comment reply) {
        notify(reply.getParent().getUser(), reply, Notification.Type.COMMENT_REPLY,
                " 님이 회원님의 댓글에 답글을 남겼습니다.");
    }

    /** 자기가 자기한테 보내는 알림은 남기지 않는다. 눌렀을 때는 댓글이 달린 영상으로 간다. */
    private void notify(User recipient, Comment comment, Notification.Type type, String suffix) {

        User writer = comment.getUser();

        if (recipient.getId().equals(writer.getId())) {
            return;
        }

        notificationRepository.save(new Notification(
                recipient,
                type,
                writer.getNickname() + suffix,
                writer.getId(),
                comment.getStream().getId()
        ));
    }

    public PageResponse<NotificationResponse> findMine(String email, Pageable pageable) {

        User user = findUser(email);

        return PageResponse.from(
                notificationRepository
                        .findByRecipientIdOrderByCreatedAtDescIdDesc(user.getId(), pageable)
                        .map(NotificationResponse::from)
        );
    }

    public long unreadCount(String email) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(findUser(email).getId());
    }

    @Transactional
    public void markAsRead(Long notificationId, String email) {

        Notification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, findUser(email).getId())
                .orElseThrow(() -> new NotFoundException("알림을 찾을 수 없습니다."));

        notification.markAsRead();
    }

    @Transactional
    public int markAllAsRead(String email) {
        return notificationRepository.markAllAsRead(findUser(email).getId());
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }
}
