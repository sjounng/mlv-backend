package kr.maribel.backend.repository;

import java.util.List;
import kr.maribel.backend.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByOrderByPinnedDescCreatedAtDesc();
}
