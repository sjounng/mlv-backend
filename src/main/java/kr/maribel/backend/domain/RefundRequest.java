package kr.maribel.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import kr.maribel.backend.domain.DomainEnums.RefundStatus;

@Entity
@Table(name = "refund_requests")
public class RefundRequest extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_charge_id", nullable = false)
    private CashCharge cashCharge;

    @Lob
    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundStatus status = RefundStatus.REQUESTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private AdminAccount processedBy;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "operator_memo", length = 500)
    private String operatorMemo;

    protected RefundRequest() {
    }

    public RefundRequest(CashCharge cashCharge, String reason) {
        this.cashCharge = cashCharge;
        this.reason = reason;
    }

    public void process(RefundStatus status, AdminAccount processedBy, String operatorMemo) {
        this.status = status;
        this.processedBy = processedBy;
        this.operatorMemo = operatorMemo;
        this.processedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CashCharge getCashCharge() {
        return cashCharge;
    }

    public String getReason() {
        return reason;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public AdminAccount getProcessedBy() {
        return processedBy;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public String getOperatorMemo() {
        return operatorMemo;
    }
}
