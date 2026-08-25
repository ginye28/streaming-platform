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

    @EntityGraph(attributePaths = {"user", "category"})
    Optional<Stream> findWithUserById(Long id);

    /** 채널(특정 사용자)의 영상 목록. */
    @EntityGraph(attributePaths = {"user", "category"})
    Page<Stream> findByUserId(Long userId, Pageable pageable);

    /** 구독 중인 채널들의 영상 피드. */
    @EntityGraph(attributePaths = {"user", "category"})
    Page<Stream> findByUserIdIn(Collection<Long> userIds, Pageable pageable);

    long countByUserId(Long userId);

    /**
     * 전체 목록. 카테고리로 좁힐 수 있고, 내가 차단한 사용자의 영상은 제외한다.
     * excludedUserIds 는 비어 있으면 안 된다 — JPQL 의 NOT IN 은 빈 컬렉션을 받을 수 없어서,
     * 차단한 사용자가 없을 때는 호출부가 절대 존재할 수 없는 id(-1L)를 채워 넣는다.
     */
    @EntityGraph(attributePaths = {"user", "category"})
    @Query("""
            select s from Stream s
            where (:categoryId is null or s.category.id = :categoryId)
              and s.user.id not in :excludedUserIds
            """)
    Page<Stream> findAllFiltered(
            @Param("categoryId") Long categoryId,
            @Param("excludedUserIds") Collection<Long> excludedUserIds,
            Pageable pageable);

    /** 제목뿐 아니라 설명도 검색 대상에 넣는다. */
    @EntityGraph(attributePaths = {"user", "category"})
    @Query("""
            select s from Stream s
            where lower(s.title) like lower(concat('%', :keyword, '%'))
               or lower(s.description) like lower(concat('%', :keyword, '%'))
            """)
    Page<Stream> search(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "category"})
    List<Stream> findTop10ByOrderByViewCountDesc();

    @EntityGraph(attributePaths = {"user", "category"})
    List<Stream> findTop10ByOrderByCreatedAtDesc();

    /**
     * 조회수를 DB 에서 직접 증가시킨다.
     * 엔티티를 읽어 +1 하고 저장하면 동시 조회 시 증가분이 유실된다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Stream s set s.viewCount = s.viewCount + 1 where s.id = :id")
    int increaseViewCount(@Param("id") Long id);
}
