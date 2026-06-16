package kr.maribel.backend.repository;

import java.util.List;
import kr.maribel.backend.domain.DomainEnums.RefundStatus;
import kr.maribel.backend.domain.RefundRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    @EntityGraph(attributePaths = {"cashCharge", "processedBy"})
    List<RefundRequest> findTop50ByOrderByCreatedAtDesc();

    long countByStatus(RefundStatus status);
}
