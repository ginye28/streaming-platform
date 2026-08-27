package com.sp.api.vtuber.repository;

import com.sp.api.vtuber.entity.ModelCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ModelCreditRepository extends JpaRepository<ModelCredit, Long> {

    /** 본인이 정한 차례대로. */
    @Query("select c from ModelCredit c where c.user.id = :userId order by c.position asc, c.id asc")
    List<ModelCredit> findByUserId(@Param("userId") Long userId);

    /** 저장할 때는 통째로 갈아 끼운다. 줄마다 수정·삭제를 따지는 것보다 단순하다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ModelCredit c where c.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
