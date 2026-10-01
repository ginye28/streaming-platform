package com.sp.api.payment.service;

import com.sp.api.common.exception.NotFoundException;
import com.sp.api.payment.config.PaymentProperties;
import com.sp.api.payment.dto.EarningsResponse;
import com.sp.api.payment.entity.PaymentStatus;
import com.sp.api.payment.repository.MonthlyTotal;
import com.sp.api.payment.repository.PaymentRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 채널 주인의 수익 장부. 내 채널로 들어온 승인된 결제를 달별로 합산한다.
 *
 * 누가 결제했는지는 담지 않는다. 채널 주인이 알 필요가 없는 개인 정보라서 금액과 건수만 보여 준다.
 * 환불은 승인된 달에서 뺀다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EarningsService {

    private final PaymentProperties properties;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public EarningsResponse earnings(String email) {

        User me = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        List<MonthlyTotal> rows = paymentRepository.monthlyTotals(
                me.getId(), List.of(PaymentStatus.DONE, PaymentStatus.CANCELED));

        // "2026-10" 같은 달 이름으로 묶는다. 최근 달이 위로 오게 거꾸로 정렬한다.
        Map<String, long[]> byMonth = new TreeMap<>(Comparator.reverseOrder());

        for (MonthlyTotal row : rows) {
            String month = "%04d-%02d".formatted(row.year(), row.month());

            // [0]=gross [1]=refunded [2]=count
            long[] sums = byMonth.computeIfAbsent(month, key -> new long[3]);

            sums[0] += row.amount();
            sums[2] += row.count();

            if (row.status() == PaymentStatus.CANCELED) {
                sums[1] += row.amount();
            }
        }

        int feePercent = properties.getPlatformFeePercent();

        List<EarningsResponse.Month> months = byMonth.entrySet().stream()
                .map(entry -> month(entry.getKey(), entry.getValue(), feePercent))
                .toList();

        long gross = months.stream().mapToLong(EarningsResponse.Month::gross).sum();
        long refunded = months.stream().mapToLong(EarningsResponse.Month::refunded).sum();
        long count = months.stream().mapToLong(EarningsResponse.Month::count).sum();
        long fee = months.stream().mapToLong(EarningsResponse.Month::fee).sum();

        return new EarningsResponse(
                feePercent,
                new EarningsResponse.Totals(gross, refunded, fee, gross - refunded - fee, count),
                months
        );
    }

    private static EarningsResponse.Month month(String name, long[] sums, int feePercent) {

        long gross = sums[0];
        long refunded = sums[1];
        long fee = (gross - refunded) * feePercent / 100;

        return new EarningsResponse.Month(name, gross, refunded, fee, gross - refunded - fee, sums[2]);
    }
}
