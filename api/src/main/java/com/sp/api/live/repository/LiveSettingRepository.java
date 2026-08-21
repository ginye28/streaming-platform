package com.sp.api.live.repository;

import com.sp.api.live.entity.LiveSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LiveSettingRepository extends JpaRepository<LiveSetting, Long> {

    Optional<LiveSetting> findByUserId(Long userId);
}
