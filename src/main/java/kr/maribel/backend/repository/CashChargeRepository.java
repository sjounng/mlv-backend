package kr.maribel.backend.repository;

import java.util.List;
import java.util.Optional;
import kr.maribel.backend.domain.CashCharge;
import kr.maribel.backend.domain.DomainEnums.ChargeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashChargeRepository extends JpaRepository<CashCharge, Long> {

    Optional<CashCharge> findByMerchantOrderId(String merchantOrderId);

    Optional<CashCharge> findByStellaPaymentId(String stellaPaymentId);

    List<CashCharge> findTop50ByMemberIdOrderByCreatedAtDesc(Long memberId);

    long countByStatus(ChargeStatus status);

    Page<CashCharge> findAllByStatus(ChargeStatus status, Pageable pageable);

    // 결제 완료(PAID) 금액 합계(원) — 관리자 프로필 조회의 후원금액
    @Query("select coalesce(sum(c.paymentAmountKrw), 0) from CashCharge c where c.member.id = :memberId and c.status = kr.maribel.backend.domain.DomainEnums.ChargeStatus.PAID")
    long sumPaidKrwByMemberId(@Param("memberId") Long memberId);
}
