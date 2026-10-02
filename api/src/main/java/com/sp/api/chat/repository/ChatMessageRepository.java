package com.sp.api.chat.repository;

import com.sp.api.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 응답이 작성자 닉네임을 읽으므로 함께 조회한다.
    @EntityGraph(attributePaths = "user")
    Page<ChatMessage> findByLiveStreamIdOrderByIdDesc(Long liveStreamId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "liveStream"})
    Optional<ChatMessage> findWithUserById(Long id);

    /** 후원 결제가 환불되면 그 후원 메시지를 찾는다. */
    Optional<ChatMessage> findByDonationPaymentId(Long paymentId);

    /**
     * 다시보기: 지운 메시지를 뺀 채팅을 오래된 것부터, afterId 다음부터 읽는다.
     * 방송이 길면 한 번에 다 읽지 않도록 화면이 afterId 로 이어서 받아 간다.
     */
    @EntityGraph(attributePaths = "user")
    @Query("""
            select m from ChatMessage m
            where m.liveStream.id = :liveId and m.id > :afterId and m.deleted = false
            order by m.id asc
            """)
    List<ChatMessage> findReplay(
            @Param("liveId") Long liveId, @Param("afterId") Long afterId, Pageable pageable);

    /** 한 사람이 이 방송에서 쓴 메시지. 강퇴하면서 함께 지울 때 쓴다. */
    List<ChatMessage> findByLiveStreamIdAndUserId(Long liveStreamId, Long userId);
}
