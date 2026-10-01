package com.sp.api.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 유료 만료 임박 알림을 한 시간마다 돌린다.
 *
 * 테스트에서는 꺼 둔다(app.membership.expiry-scheduler.enabled=false). 켜 두면 테스트가 쓰는 DB 를
 * 백그라운드에서 건드려 테스트가 흔들린다.
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.membership.expiry-scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class PaidExpiryScheduler {

    private final PaidExpiryService paidExpiryService;

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT1H")
    public void run() {
        try {
            paidExpiryService.notifyExpiring();
        } catch (RuntimeException e) {
            // 한 번 실패해도 다음 시간에 다시 돈다. 서버는 멈추지 않는다.
            log.error("유료 만료 임박 알림 실패", e);
        }
    }
}
