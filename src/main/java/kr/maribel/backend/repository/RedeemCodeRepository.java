package kr.maribel.backend.repository;

import java.util.Optional;
import kr.maribel.backend.domain.RedeemCode;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RedeemCodeRepository extends JpaRepository<RedeemCode, Long> {

    @EntityGraph(attributePaths = "mailTemplate")
    Optional<RedeemCode> findByCodeIgnoreCase(String code);
}
