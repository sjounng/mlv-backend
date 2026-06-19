package kr.maribel.backend.repository;

import java.util.List;
import kr.maribel.backend.domain.EventParticipation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventParticipationRepository extends JpaRepository<EventParticipation, Long> {

    boolean existsByMemberIdAndEventIdAndClaimKey(Long memberId, Long eventId, String claimKey);

    @Query("select p.claimKey from EventParticipation p where p.member.id = :memberId and p.event.id = :eventId")
    List<String> findClaimKeys(@Param("memberId") Long memberId, @Param("eventId") Long eventId);

    @EntityGraph(attributePaths = {"event", "outboundMail"})
    List<EventParticipation> findTop50ByMemberIdOrderByClaimedAtDesc(Long memberId);
}
