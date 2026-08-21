package com.sp.api.stream.repository;

import com.sp.api.stream.entity.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StreamRepository extends JpaRepository<Stream, Long> {

    // StreamResponse 가 user.nickname 을 읽으므로 함께 조회해 N+1 을 막는다.
    @Override
    @EntityGraph(attributePaths = "user")
    Page<Stream> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Optional<Stream> findWithUserById(Long id);

    @EntityGraph(attributePaths = "user")
    Page<Stream> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

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
