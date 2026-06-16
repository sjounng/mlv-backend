package kr.maribel.backend.repository;

import java.util.List;
import kr.maribel.backend.domain.RedeemCodeUsage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RedeemCodeUsageRepository extends JpaRepository<RedeemCodeUsage, Long> {

    boolean existsByRedeemCodeIdAndMemberId(Long redeemCodeId, Long memberId);

    @EntityGraph(attributePaths = {"redeemCode", "outboundMail"})
    List<RedeemCodeUsage> findTop50ByMemberIdOrderByUsedAtDesc(Long memberId);
}
