package kr.maribel.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import kr.maribel.backend.domain.DomainEnums.EventType;

@Entity
@Table(name = "events", indexes = @Index(name = "idx_events_active_period", columnList = "active,start_at,end_at"))
public class MaribelEvent extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventType type;

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mail_template_id", nullable = false)
    private MailTemplate mailTemplate;

    @Column(nullable = false)
    private boolean active = true;

    protected MaribelEvent() {
    }

    public MaribelEvent(String name, EventType type, String description, Instant startAt, Instant endAt, MailTemplate mailTemplate) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.mailTemplate = mailTemplate;
    }

    public void update(String name, EventType type, String description, Instant startAt, Instant endAt,
                       MailTemplate mailTemplate, boolean active) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.mailTemplate = mailTemplate;
        this.active = active;
    }

    public boolean isClaimable(Instant now) {
        return active && !now.isBefore(startAt) && !now.isAfter(endAt);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public EventType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public MailTemplate getMailTemplate() {
        return mailTemplate;
    }

    public boolean isActive() {
        return active;
    }
}
