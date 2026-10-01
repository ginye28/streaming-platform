package com.sp.api.payment.repository;

import com.sp.api.payment.entity.Payment;
import com.sp.api.payment.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"user", "channel"})
    Optional<Payment> findByOrderId(String orderId);

    /**
     * 승인 결과를 반영할 때 쓴다. 같은 주문의 승인 요청이 동시에 두 번 들어와도
     * 구독 기간이 두 번 늘어나지 않도록, 먼저 잡은 쪽이 끝날 때까지 다른 쪽이 기다린다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.orderId = :orderId")
    Optional<Payment> findByOrderIdForUpdate(@Param("orderId") String orderId);

    @EntityGraph(attributePaths = {"user", "channel"})
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findWithUsersById(@Param("id") Long id);

    /** 내 결제 내역. 승인된 것과 환불된 것을 최신순으로. */
    @EntityGraph(attributePaths = "channel")
    Page<Payment> findByUserIdAndStatusInOrderByIdDesc(
            Long userId, Collection<PaymentStatus> statuses, Pageable pageable);

    /** 관리자 목록. */
    @EntityGraph(attributePaths = {"user", "channel"})
    Page<Payment> findAllByOrderByIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "channel"})
    Page<Payment> findByStatusOrderByIdDesc(PaymentStatus status, Pageable pageable);

    /** 내 채널로 들어온 결제를 승인된 달·상태별로 합산한다. 수익 장부용. */
    @Query("""
            select new com.sp.api.payment.repository.MonthlyTotal(
                year(p.approvedAt), month(p.approvedAt), p.status, count(p), sum(p.amount))
            from Payment p
            where p.channel.id = :channelId and p.status in :statuses and p.approvedAt is not null
            group by year(p.approvedAt), month(p.approvedAt), p.status
            """)
    List<MonthlyTotal> monthlyTotals(
            @Param("channelId") Long channelId, @Param("statuses") Collection<PaymentStatus> statuses);
}
