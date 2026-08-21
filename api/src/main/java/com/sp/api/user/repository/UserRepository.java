package com.sp.api.user.repository;

import com.sp.api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<User> findByEmail(String email);

    Optional<User> findByStreamKey(String streamKey);

    Optional<User> findByPublicName(String publicName);
}
