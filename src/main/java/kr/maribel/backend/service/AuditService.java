package kr.maribel.backend.service;

import kr.maribel.backend.domain.AuditLog;
import kr.maribel.backend.repository.AuditLogRepository;
import kr.maribel.backend.security.AuthenticatedPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(AuthenticatedPrincipal principal, String entityType, String entityId, String action, String oldValue, String newValue) {
        String actor = principal == null
                ? "system"
                : principal.role() + ":" + (principal.adminId() != null ? principal.adminId() : principal.memberId());
        auditLogRepository.save(new AuditLog(actor, entityType, entityId, action, oldValue, newValue));
    }

    @Transactional
    public void recordSystem(String entityType, String entityId, String action, String oldValue, String newValue) {
        auditLogRepository.save(new AuditLog("system", entityType, entityId, action, oldValue, newValue));
    }
}
