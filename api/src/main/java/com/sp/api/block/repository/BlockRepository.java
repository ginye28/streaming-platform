package com.sp.api.block.repository;

import com.sp.api.block.entity.Block;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {

    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /** 내가 차단한 사용자 목록. */
    @EntityGraph(attributePaths = "blocked")
    Page<Block> findByBlockerId(Long blockerId, Pageable pageable);

    /** 피드에서 걸러낼 차단 대상 id 목록. */
    @Query("select b.blocked.id from Block b where b.blocker.id = :blockerId")
    List<Long> findBlockedUserIdsByBlockerId(@Param("blockerId") Long blockerId);
}
