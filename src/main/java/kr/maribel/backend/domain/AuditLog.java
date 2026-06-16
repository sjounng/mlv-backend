package kr.maribel.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_actor_created", columnList = "actor,created_at"),
        @Index(name = "idx_audit_logs_entity", columnList = "entity_type,entity_id")
})
public class AuditLog extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String actor;

    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 120)
    private String entityId;

    @Column(nullable = false, length = 80)
    private String action;

    @Lob
    @Column(name = "old_value")
    private String oldValue;

    @Lob
    @Column(name = "new_value")
    private String newValue;

    protected AuditLog() {
    }

    public AuditLog(String actor, String entityType, String entityId, String action, String oldValue, String newValue) {
        this.actor = actor;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Long getId() {
        return id;
    }

    public String getActor() {
        return actor;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getAction() {
        return action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }
}
