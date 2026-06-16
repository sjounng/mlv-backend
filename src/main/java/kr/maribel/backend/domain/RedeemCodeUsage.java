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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "redeem_code_usages",
        indexes = @Index(name = "idx_redeem_code_usages_user", columnList = "user_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_redeem_code_usages_once", columnNames = {"code_id", "user_id"})
)
public class RedeemCodeUsage extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_id", nullable = false)
    private RedeemCode redeemCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @Column(name = "used_at", nullable = false)
    private Instant usedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_mail_id")
    private OutboundMail outboundMail;

    protected RedeemCodeUsage() {
    }

    public RedeemCodeUsage(RedeemCode redeemCode, Member member) {
        this.redeemCode = redeemCode;
        this.member = member;
        this.usedAt = Instant.now();
    }

    public void attachMail(OutboundMail outboundMail) {
        this.outboundMail = outboundMail;
    }

    public Long getId() {
        return id;
    }

    public RedeemCode getRedeemCode() {
        return redeemCode;
    }

    public Member getMember() {
        return member;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public OutboundMail getOutboundMail() {
        return outboundMail;
    }
}
