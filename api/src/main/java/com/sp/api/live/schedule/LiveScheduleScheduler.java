package com.sp.api.live.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 방송 예약의 "곧 시작" 알림을 보내고, 지난 예약을 내린다. 1분마다 돈다.
 *
 * 무료 서버는 15분 동안 요청이 없으면 잠들기 때문에, 잠든 사이에는 이 작업도 멈춘다.
 * (UptimeRobot 이 5분마다 깨워 두는 동안은 돈다.) 테스트에서는 꺼 둔다.
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.live.schedule-scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class LiveScheduleScheduler {

    private final LiveScheduleService scheduleService;

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT1M")
    public void run() {
        try {
            int reminded = scheduleService.sendReminders();
            int expired = scheduleService.expireStale();

            if (reminded > 0 || expired > 0) {
                log.info("방송 예약 정리: 곧 시작 알림 {}건, 만료 {}건", reminded, expired);
            }

        } catch (RuntimeException e) {
            // 한 번 실패해도 다음 분에 다시 돈다. 서버는 멈추지 않는다.
            log.error("방송 예약 작업 실패", e);
        }
    }
}
