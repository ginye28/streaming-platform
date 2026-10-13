package com.sp.api.auth.repository;

import com.sp.api.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByTokenHash(String tokenHash);

    /** 한 사용자의 리프레시 토큰을 모두 지운다(비밀번호를 바꿀 때). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken t where t.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
