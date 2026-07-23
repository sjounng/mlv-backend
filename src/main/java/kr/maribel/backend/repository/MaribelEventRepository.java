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

    // 공개(게시된) 이벤트 목록 — 게시일(최신) 순. 검색/페이지네이션은 서비스에서 처리.
    @EntityGraph(attributePaths = "mailTemplate")
    List<MaribelEvent> findByActiveTrueOrderByCreatedAtDesc();

    // 상단 슬라이더용 featured 이벤트
    @EntityGraph(attributePaths = "mailTemplate")
    List<MaribelEvent> findByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc();

    // 공개 상세 조회 (게시된 것만)
    @EntityGraph(attributePaths = "mailTemplate")
    Optional<MaribelEvent> findByIdAndActiveTrue(Long id);

    @EntityGraph(attributePaths = "mailTemplate")
    @Query("""
            select e from MaribelEvent e
            where e.active = true and e.startAt <= :now and e.endAt >= :now
            order by e.startAt desc
            """)
    List<MaribelEvent> findActiveEvents(@Param("now") Instant now);
}
