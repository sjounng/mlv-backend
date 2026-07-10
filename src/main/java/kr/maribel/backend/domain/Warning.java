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
import kr.maribel.backend.domain.DomainEnums.WarningReason;

/**
 * 유저에게 부여된 경고 1건. 관리자가 사유(사건경위)를 남기고, 취소 가능(로그 보존).
 * 유효(취소 안 된) 경고 수가 3회 이상이면 악성 유저로 분류된다.
 */
@Entity
@Table(name = "warnings", indexes = @Index(name = "idx_warnings_user_created", columnList = "user_id,created_at"))
public class Warning extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WarningReason reason;

    // 관리자가 작성하는 사건경위(내부 로그/조회용). 모든 경고에 필수.
    @Lob
    @Column(name = "detail", nullable = false)
    private String detail;

    // CUSTOM(직접작성)일 때 유저 메일에 노출되는 "직접 작성 내용"
    @Column(name = "custom_text", length = 500)
    private String customText;

    // 발부 당시 누적 유효 경고 수 (메일 "현재 n회" 표기용 기록)
    @Column(name = "count_at_issue", nullable = false)
    private int countAtIssue;

    // 부여한 관리자 식별(감사용)
    @Column(name = "issued_by", length = 80)
    private String issuedBy;

    @Column(nullable = false)
    private boolean canceled = false;

    @Column(name = "canceled_reason", length = 300)
    private String canceledReason;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    protected Warning() {
    }

    public Warning(Member member, WarningReason reason, String detail, String customText,
                   int countAtIssue, String issuedBy) {
        this.member = member;
        this.reason = reason;
        this.detail = detail;
        this.customText = customText;
        this.countAtIssue = countAtIssue;
        this.issuedBy = issuedBy;
    }

    public void cancel(String reason) {
        this.canceled = true;
        this.canceledReason = reason;
        this.canceledAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public WarningReason getReason() {
        return reason;
    }

    public String getDetail() {
        return detail;
    }

    public String getCustomText() {
        return customText;
    }

    public int getCountAtIssue() {
        return countAtIssue;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public String getCanceledReason() {
        return canceledReason;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }
}
