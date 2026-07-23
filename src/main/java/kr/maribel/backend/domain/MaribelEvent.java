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
import kr.maribel.backend.domain.DomainEnums.EventStatus;
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
    private EventType type = EventType.GENERAL;

    // 목록/상세 상단에 쓰이는 대표 배너 이미지 (07-22 목록형 개편)
    @Column(name = "banner_image_url", length = 500)
    private String bannerImageUrl;

    // 본문(이미지/영상/굵게 등 리치 HTML)
    @Lob
    private String description;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    // 관리자가 게시 시 수동 선택하는 진행 상태 (목록 우측에 표시)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.ONGOING;

    // 상단 슬라이더 노출 여부 (관리자가 선택한 이벤트의 배너가 슬라이드로 표시됨)
    @Column(nullable = false)
    private boolean featured = false;

    // 보상 수령형(출석/리딤 등) 이벤트에서만 사용. 콘텐츠형 이벤트는 null.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mail_template_id")
    private MailTemplate mailTemplate;

    // 공개 여부 (비공개 시 목록/상세에서 숨김)
    @Column(nullable = false)
    private boolean active = true;

    protected MaribelEvent() {
    }

    public MaribelEvent(String name, EventType type, String bannerImageUrl, String description,
                        Instant startAt, Instant endAt, EventStatus status, boolean featured,
                        MailTemplate mailTemplate, boolean active) {
        this.name = name;
        this.type = type == null ? EventType.GENERAL : type;
        this.bannerImageUrl = bannerImageUrl;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status == null ? EventStatus.ONGOING : status;
        this.featured = featured;
        this.mailTemplate = mailTemplate;
        this.active = active;
    }

    // 게시일(createdAt) 및 보상 템플릿 연결(mailTemplate)은 등록 후 수정하지 않는다. (07-22 피드백: 게시일 변경불가)
    public void update(String name, EventType type, String bannerImageUrl, String description,
                       Instant startAt, Instant endAt, EventStatus status, boolean featured,
                       MailTemplate mailTemplate, boolean active) {
        this.name = name;
        this.type = type == null ? EventType.GENERAL : type;
        this.bannerImageUrl = bannerImageUrl;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status == null ? EventStatus.ONGOING : status;
        this.featured = featured;
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

    public String getBannerImageUrl() {
        return bannerImageUrl;
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

    public EventStatus getStatus() {
        return status;
    }

    public boolean isFeatured() {
        return featured;
    }

    public MailTemplate getMailTemplate() {
        return mailTemplate;
    }

    public boolean isActive() {
        return active;
    }
}
