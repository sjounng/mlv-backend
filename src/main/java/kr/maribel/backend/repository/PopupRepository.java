package kr.maribel.backend.repository;

import java.time.Instant;
import java.util.List;
import kr.maribel.backend.domain.Popup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PopupRepository extends JpaRepository<Popup, Long> {

    @Query("""
            select p from Popup p
            where p.active = true and p.startAt <= :now and p.endAt >= :now
            order by p.createdAt desc
            """)
    List<Popup> findVisible(@Param("now") Instant now);

    List<Popup> findAllByOrderByStartAtDesc();
}
