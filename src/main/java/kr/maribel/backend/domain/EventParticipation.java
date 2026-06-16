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
        name = "event_participations",
        indexes = @Index(name = "idx_event_participations_user", columnList = "user_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_event_participations_once", columnNames = {"user_id", "event_id", "claim_key"})
)
public class EventParticipation extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private MaribelEvent event;

    @Column(name = "claim_key", nullable = false, length = 80)
    private String claimKey;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_mail_id")
    private OutboundMail outboundMail;

    protected EventParticipation() {
    }

    public EventParticipation(Member member, MaribelEvent event, String claimKey) {
        this.member = member;
        this.event = event;
        this.claimKey = claimKey;
        this.claimedAt = Instant.now();
    }

    public void attachMail(OutboundMail outboundMail) {
        this.outboundMail = outboundMail;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public MaribelEvent getEvent() {
        return event;
    }

    public String getClaimKey() {
        return claimKey;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public OutboundMail getOutboundMail() {
        return outboundMail;
    }
}
