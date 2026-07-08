package kr.maribel.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import kr.maribel.backend.domain.DomainEnums.BannerPlacement;

@Entity
@Table(name = "popups", indexes = @Index(name = "idx_popups_active_period", columnList = "active,start_at,end_at"))
public class Popup extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    // 노출 위치: 홈 인트로 슬라이더 / 이벤트 페이지 상단
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BannerPlacement placement = BannerPlacement.EVENT;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(nullable = false)
    private boolean active = true;

    protected Popup() {
    }

    public Popup(String imageUrl, String linkUrl, BannerPlacement placement, Instant startAt, Instant endAt) {
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.placement = placement;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void update(String imageUrl, String linkUrl, BannerPlacement placement, Instant startAt, Instant endAt, boolean active) {
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.placement = placement;
        this.startAt = startAt;
        this.endAt = endAt;
        this.active = active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public BannerPlacement getPlacement() {
        return placement;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public boolean isActive() {
        return active;
    }
}
