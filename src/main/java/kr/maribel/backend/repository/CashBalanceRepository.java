package kr.maribel.backend.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import kr.maribel.backend.domain.CashBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashBalanceRepository extends JpaRepository<CashBalance, Long> {

    Optional<CashBalance> findByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from CashBalance b where b.member.id = :memberId")
    Optional<CashBalance> findByMemberIdForUpdate(@Param("memberId") Long memberId);
}
