package com.sp.api.intro.repository;

import com.sp.api.intro.entity.IntroImpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IntroImpressionRepository extends JpaRepository<IntroImpression, Long> {

    /**
     * 이름으로 만드는 쿼리를 쓰면 channel 을 타고 가는 경로를 잡지 못할 수 있어
     * JPQL 을 직접 적는다.
     */
    @Query("select i from IntroImpression i where i.viewerKey = :viewerKey and i.channel.id = :channelId")
    Optional<IntroImpression> findByViewerKeyAndChannelId(
            @Param("viewerKey") String viewerKey, @Param("channelId") Long channelId);

    boolean existsByViewerKey(String viewerKey);

    /** 이 시청자가 이미 인트로를 본 채널들. "이어보기" 에서 건너뛸 대상이다. */
    @Query("select i.channel.id from IntroImpression i where i.viewerKey = :viewerKey")
    List<Long> findSeenChannelIds(@Param("viewerKey") String viewerKey);

    /** 넘긴(PASS) 채널들. 목록에서 뒤로 미룰 때 쓴다. */
    @Query("""
            select i.channel.id from IntroImpression i
            where i.viewerKey = :viewerKey and i.action = com.sp.api.intro.entity.IntroImpression.Action.PASS
            """)
    List<Long> findPassedChannelIds(@Param("viewerKey") String viewerKey);
}
