package kr.maribel.backend.repository;

import java.util.List;
import kr.maribel.backend.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
}
