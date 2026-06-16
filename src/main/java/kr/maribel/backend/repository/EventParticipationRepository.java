package kr.maribel.backend.repository;

import java.util.List;
import kr.maribel.backend.domain.EventParticipation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventParticipationRepository extends JpaRepository<EventParticipation, Long> {

    boolean existsByMemberIdAndEventIdAndClaimKey(Long memberId, Long eventId, String claimKey);

    @EntityGraph(attributePaths = {"event", "outboundMail"})
    List<EventParticipation> findTop50ByMemberIdOrderByClaimedAtDesc(Long memberId);
}
