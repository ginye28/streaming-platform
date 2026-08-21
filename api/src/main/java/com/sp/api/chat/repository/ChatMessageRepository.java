package com.sp.api.chat.repository;

import com.sp.api.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 응답이 작성자 닉네임을 읽으므로 함께 조회한다.
    @EntityGraph(attributePaths = "user")
    Page<ChatMessage> findByLiveStreamIdOrderByIdDesc(Long liveStreamId, Pageable pageable);
}
