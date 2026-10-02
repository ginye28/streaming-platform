package com.sp.api.chat.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelBannedWordRepository extends JpaRepository<ChannelBannedWord, Long> {

    List<ChannelBannedWord> findByChannelIdOrderByIdDesc(Long channelId);

    Optional<ChannelBannedWord> findByIdAndChannelId(Long id, Long channelId);

    boolean existsByChannelIdAndWord(Long channelId, String word);

    long countByChannelId(Long channelId);
}
