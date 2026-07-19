package kr.maribel.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import kr.maribel.backend.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @EntityGraph(attributePaths = {"product", "outboundMail"})
    List<PurchaseOrder> findTop50ByMemberIdOrderByCreatedAtDesc(Long memberId);

    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);

    // 구매 제한 검증용: 기간 내(취소 제외) 구매 수량 합계
    @Query("""
            select coalesce(sum(p.quantity), 0) from PurchaseOrder p
            where p.member.id = :memberId and p.product.id = :productId
              and p.status <> kr.maribel.backend.domain.DomainEnums.PurchaseStatus.CANCELLED
              and p.createdAt >= :since
            """)
    long sumQuantitySince(@Param("memberId") Long memberId,
                          @Param("productId") Long productId,
                          @Param("since") Instant since);
}
