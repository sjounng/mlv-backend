package kr.maribel.backend.repository;

import java.util.List;
import kr.maribel.backend.domain.Warning;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarningRepository extends JpaRepository<Warning, Long> {

    List<Warning> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    long countByMemberIdAndCanceledFalse(Long memberId);
}
