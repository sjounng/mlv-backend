package kr.maribel.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "redeem_codes",
        indexes = @Index(name = "idx_redeem_codes_active", columnList = "active,expires_at"),
        uniqueConstraints = @UniqueConstraint(name = "uk_redeem_codes_code", columnNames = "code")
)
public class RedeemCode extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mail_template_id", nullable = false)
    private MailTemplate mailTemplate;

    @Column(name = "max_uses", nullable = false)
    private int maxUses;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    protected RedeemCode() {
    }

    public RedeemCode(String code, MailTemplate mailTemplate, int maxUses, Instant expiresAt) {
        this.code = code;
        this.mailTemplate = mailTemplate;
        this.maxUses = maxUses;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable(Instant now) {
        return active && usedCount < maxUses && (expiresAt == null || expiresAt.isAfter(now));
    }

    public void increaseUsedCount() {
        this.usedCount++;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public MailTemplate getMailTemplate() {
        return mailTemplate;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }
}
