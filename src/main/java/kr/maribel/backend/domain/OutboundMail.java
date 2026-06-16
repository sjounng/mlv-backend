package kr.maribel.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import kr.maribel.backend.domain.DomainEnums.MailSourceType;
import kr.maribel.backend.domain.DomainEnums.OutboundMailStatus;

@Entity
@Table(
        name = "outbound_mails",
        indexes = {
                @Index(name = "idx_outbound_mails_status_created", columnList = "status,created_at"),
                @Index(name = "idx_outbound_mails_target", columnList = "target_uuid")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_outbound_mails_idempotency", columnNames = "idempotency_key")
)
public class OutboundMail extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_uuid", nullable = false, length = 36)
    private String targetUuid;

    @Column(name = "mail_code", nullable = false, length = 80)
    private String mailCode;

    @Column(nullable = false, length = 160)
    private String subject;

    @Lob
    @Column(nullable = false)
    private String content;

    @Lob
    @Column(name = "rewards_json", nullable = false)
    private String rewardsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private MailSourceType sourceType;

    @Column(name = "source_ref_id", nullable = false, length = 120)
    private String sourceRefId;

    @Column(name = "idempotency_key", nullable = false, length = 180)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OutboundMailStatus status = OutboundMailStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    protected OutboundMail() {
    }

    public OutboundMail(String targetUuid, MailTemplate template, MailSourceType sourceType,
                        String sourceRefId, String idempotencyKey) {
        this.targetUuid = targetUuid;
        this.mailCode = template.getMailCode();
        this.subject = template.getSubject();
        this.content = template.getContent();
        this.rewardsJson = template.getRewardsJson();
        this.sourceType = sourceType;
        this.sourceRefId = sourceRefId;
        this.idempotencyKey = idempotencyKey;
    }

    public OutboundMail(String targetUuid, String mailCode, String subject, String content,
                        String rewardsJson, MailSourceType sourceType, String sourceRefId,
                        String idempotencyKey) {
        this.targetUuid = targetUuid;
        this.mailCode = mailCode;
        this.subject = subject;
        this.content = content;
        this.rewardsJson = rewardsJson;
        this.sourceType = sourceType;
        this.sourceRefId = sourceRefId;
        this.idempotencyKey = idempotencyKey;
    }

    public void markSent() {
        this.status = OutboundMailStatus.SENT;
        this.sentAt = Instant.now();
        this.lastError = null;
    }

    public void markFailed(String error, boolean retryable) {
        this.status = OutboundMailStatus.FAILED;
        this.lastError = error;
        if (retryable) {
            this.retryCount++;
            this.nextRetryAt = Instant.now().plusSeconds(Math.min(300L * retryCount, 3600L));
        }
    }

    public void retry() {
        this.status = OutboundMailStatus.PENDING;
        this.nextRetryAt = null;
        this.lastError = null;
    }

    public void cancel(String reason) {
        this.status = OutboundMailStatus.CANCELLED;
        this.lastError = reason;
    }

    public Long getId() {
        return id;
    }

    public String getTargetUuid() {
        return targetUuid;
    }

    public String getMailCode() {
        return mailCode;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public String getRewardsJson() {
        return rewardsJson;
    }

    public MailSourceType getSourceType() {
        return sourceType;
    }

    public String getSourceRefId() {
        return sourceRefId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public OutboundMailStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public String getLastError() {
        return lastError;
    }
}
