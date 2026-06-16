package kr.maribel.backend.repository;

import java.util.List;
import java.util.Optional;
import kr.maribel.backend.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @EntityGraph(attributePaths = {"product", "outboundMail"})
    List<PurchaseOrder> findTop50ByMemberIdOrderByCreatedAtDesc(Long memberId);

    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);
}
