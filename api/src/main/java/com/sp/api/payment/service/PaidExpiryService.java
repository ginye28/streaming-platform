package com.sp.api.payment.service;

import com.sp.api.notification.service.NotificationService;
import com.sp.api.payment.config.PaymentProperties;
import com.sp.api.subscribe.entity.Subscribe;
import com.sp.api.subscribe.repository.SubscribeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 유료 구독이 곧 끝나는 사람에게 알림을 보낸다. 자동 갱신이 없으니 끝나기 전에 알려 주는 것이
 * 갱신을 놓치지 않게 하는 유일한 장치다.
 *
 * 같은 만료 시각에 대해서는 한 번만 알린다(expiryNotifiedFor). 연장해서 만료 시각이 바뀌면
 * 다음 만료 때 다시 알린다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaidExpiryService {

    private final PaymentProperties properties;
    private final SubscribeRepository subscribeRepository;
    private final NotificationService notificationService;

    /** @return 이번에 알림을 보낸 사람 수 */
    @Transactional
    public int notifyExpiring() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusDays(properties.getExpiryNoticeDays());

        List<Subscribe> expiring = subscribeRepository.findExpiringPaid(now, until);

        for (Subscribe subscribe : expiring) {

            long hoursLeft = Duration.between(now, subscribe.getPaidUntil()).toHours();

            notificationService.notifyPaidExpiring(subscribe, hoursLeft);
            subscribe.markExpiryNotified();
        }

        if (!expiring.isEmpty()) {
            log.info("유료 만료 임박 알림 {}건", expiring.size());
        }

        return expiring.size();
    }
}
