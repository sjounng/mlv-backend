package kr.maribel.backend.repository;

import java.util.List;
import kr.maribel.backend.domain.CashTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashTransactionRepository extends JpaRepository<CashTransaction, Long> {

    List<CashTransaction> findTop50ByMemberIdOrderByCreatedAtDesc(Long memberId);
}
