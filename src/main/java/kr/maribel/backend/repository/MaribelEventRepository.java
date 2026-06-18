package kr.maribel.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import kr.maribel.backend.domain.MaribelEvent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaribelEventRepository extends JpaRepository<MaribelEvent, Long> {

    @EntityGraph(attributePaths = "mailTemplate")
    Optional<MaribelEvent> findWithMailTemplateById(Long id);

    @EntityGraph(attributePaths = "mailTemplate")
    List<MaribelEvent> findAllByOrderByStartAtDesc();

    @EntityGraph(attributePaths = "mailTemplate")
    @Query("""
            select e from MaribelEvent e
            where e.active = true and e.startAt <= :now and e.endAt >= :now
            order by e.startAt desc
            """)
    List<MaribelEvent> findActiveEvents(@Param("now") Instant now);
}
