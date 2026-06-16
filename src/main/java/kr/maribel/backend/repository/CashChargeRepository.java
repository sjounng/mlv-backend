package kr.maribel.backend.repository;

import java.util.List;
import java.util.Optional;
import kr.maribel.backend.domain.CashCharge;
import kr.maribel.backend.domain.DomainEnums.ChargeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashChargeRepository extends JpaRepository<CashCharge, Long> {

    Optional<CashCharge> findByMerchantOrderId(String merchantOrderId);

    Optional<CashCharge> findByStellaPaymentId(String stellaPaymentId);

    List<CashCharge> findTop50ByMemberIdOrderByCreatedAtDesc(Long memberId);

    long countByStatus(ChargeStatus status);

    Page<CashCharge> findAllByStatus(ChargeStatus status, Pageable pageable);
}
