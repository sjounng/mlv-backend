package kr.maribel.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import kr.maribel.backend.domain.DomainEnums.OutboundMailStatus;
import kr.maribel.backend.domain.OutboundMail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboundMailRepository extends JpaRepository<OutboundMail, Long> {

    Optional<OutboundMail> findByIdempotencyKey(String idempotencyKey);

    List<OutboundMail> findTop50ByTargetUuidOrderByCreatedAtDesc(String targetUuid);

    List<OutboundMail> findTop100ByOrderByCreatedAtDesc();

    long countByStatus(OutboundMailStatus status);

    @Query("""
            select m from OutboundMail m
            where m.status = :pending
               or (m.status = :failed and m.nextRetryAt is not null and m.nextRetryAt <= :now)
            order by m.createdAt asc
            """)
    List<OutboundMail> findDispatchable(@Param("pending") OutboundMailStatus pending,
                                        @Param("failed") OutboundMailStatus failed,
                                        @Param("now") Instant now,
                                        Pageable pageable);
}
