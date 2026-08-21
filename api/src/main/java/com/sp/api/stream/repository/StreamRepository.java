package com.sp.api.stream.repository;

import com.sp.api.stream.entity.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StreamRepository extends JpaRepository<Stream, Long> {

    // StreamResponse 가 user 를 읽으므로 함께 조회해 N+1 을 막는다.
    @Override
    @EntityGraph(attributePaths = "user")
    Page<Stream> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Optional<Stream> findWithUserById(Long id);

    /** 채널(특정 사용자)의 영상 목록. */
    @EntityGraph(attributePaths = "user")
    Page<Stream> findByUserId(Long userId, Pageable pageable);

    /** 구독 중인 채널들의 영상 피드. */
    @EntityGraph(attributePaths = "user")
    Page<Stream> findByUserIdIn(Collection<Long> userIds, Pageable pageable);

    long countByUserId(Long userId);

    /** 제목뿐 아니라 설명도 검색 대상에 넣는다. */
    @EntityGraph(attributePaths = "user")
    @Query("""
            select s from Stream s
            where lower(s.title) like lower(concat('%', :keyword, '%'))
               or lower(s.description) like lower(concat('%', :keyword, '%'))
            """)
    Page<Stream> search(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    List<Stream> findTop10ByOrderByViewCountDesc();

    @EntityGraph(attributePaths = "user")
    List<Stream> findTop10ByOrderByCreatedAtDesc();

    /**
     * 조회수를 DB 에서 직접 증가시킨다.
     * 엔티티를 읽어 +1 하고 저장하면 동시 조회 시 증가분이 유실된다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Stream s set s.viewCount = s.viewCount + 1 where s.id = :id")
    int increaseViewCount(@Param("id") Long id);
}
