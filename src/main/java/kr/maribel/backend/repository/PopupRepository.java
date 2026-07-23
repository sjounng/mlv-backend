package kr.maribel.backend.repository;

import java.time.Instant;
import java.util.List;
import kr.maribel.backend.domain.DomainEnums.BannerPlacement;
import kr.maribel.backend.domain.Popup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PopupRepository extends JpaRepository<Popup, Long> {

    @Query("""
            select p from Popup p
            where p.active = true and p.startAt <= :now and p.endAt >= :now
            order by p.sortOrder asc, p.createdAt desc
            """)
    List<Popup> findVisible(@Param("now") Instant now);

    @Query("""
            select p from Popup p
            where p.active = true and p.placement = :placement and p.startAt <= :now and p.endAt >= :now
            order by p.sortOrder asc, p.createdAt desc
            """)
    List<Popup> findVisibleByPlacement(@Param("placement") BannerPlacement placement, @Param("now") Instant now);

    // 관리자 목록: 노출 순서 → 최신순
    List<Popup> findAllByOrderBySortOrderAscCreatedAtDesc();
}
