package kr.maribel.backend.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import kr.maribel.backend.domain.CashCharge;
import kr.maribel.backend.domain.DomainEnums.ChargeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashChargeRepository extends JpaRepository<CashCharge, Long> {

    Optional<CashCharge> findByMerchantOrderId(String merchantOrderId);

    // 웹훅 동시/재전송 시 이중 지급 방지를 위한 행 잠금 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CashCharge c where c.merchantOrderId = :merchantOrderId")
    Optional<CashCharge> findByMerchantOrderIdForUpdate(@Param("merchantOrderId") String merchantOrderId);

    // 환불 처리 시 이중 차감 방지를 위한 행 잠금 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CashCharge c where c.id = :id")
    Optional<CashCharge> findByIdForUpdate(@Param("id") Long id);

    Optional<CashCharge> findByStellaPaymentId(String stellaPaymentId);

    List<CashCharge> findTop50ByMemberIdOrderByCreatedAtDesc(Long memberId);

    long countByStatus(ChargeStatus status);

    Page<CashCharge> findAllByStatus(ChargeStatus status, Pageable pageable);

    // 결제 완료(PAID) 금액 합계(원) — 관리자 프로필 조회의 후원금액
    @Query("select coalesce(sum(c.paymentAmountKrw), 0) from CashCharge c where c.member.id = :memberId and c.status = kr.maribel.backend.domain.DomainEnums.ChargeStatus.PAID")
    long sumPaidKrwByMemberId(@Param("memberId") Long memberId);
}
