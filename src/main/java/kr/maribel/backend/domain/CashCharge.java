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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import kr.maribel.backend.domain.DomainEnums.ChargeStatus;

@Entity
@Table(
        name = "cash_charges",
        indexes = @Index(name = "idx_cash_charges_user_created", columnList = "user_id,created_at"),
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cash_charges_order_id", columnNames = "merchant_order_id"),
                @UniqueConstraint(name = "uk_cash_charges_stella_payment", columnNames = "stella_payment_id")
        }
)
public class CashCharge extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_order_id", nullable = false, length = 80)
    private String merchantOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @Column(name = "cash_amount", nullable = false)
    private long cashAmount;

    @Column(name = "payment_amount_krw", nullable = false)
    private long paymentAmountKrw;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChargeStatus status = ChargeStatus.READY;

    @Column(name = "stella_payment_id", length = 120)
    private String stellaPaymentId;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Column(name = "paid_at")
    private Instant paidAt;

    protected CashCharge() {
    }

    public CashCharge(String merchantOrderId, Member member, long cashAmount, long paymentAmountKrw) {
        this.merchantOrderId = merchantOrderId;
        this.member = member;
        this.cashAmount = cashAmount;
        this.paymentAmountKrw = paymentAmountKrw;
    }

    public void markPaid(String stellaPaymentId, String receiptUrl) {
        this.status = ChargeStatus.PAID;
        this.stellaPaymentId = stellaPaymentId;
        this.receiptUrl = receiptUrl;
        this.paidAt = Instant.now();
    }

    public void markFailed() {
        this.status = ChargeStatus.FAILED;
    }

    public void markRefunded() {
        this.status = ChargeStatus.REFUNDED;
    }

    public Long getId() {
        return id;
    }

    public String getMerchantOrderId() {
        return merchantOrderId;
    }

    public Member getMember() {
        return member;
    }

    public long getCashAmount() {
        return cashAmount;
    }

    public long getPaymentAmountKrw() {
        return paymentAmountKrw;
    }

    public ChargeStatus getStatus() {
        return status;
    }

    public String getStellaPaymentId() {
        return stellaPaymentId;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }

    public Instant getPaidAt() {
        return paidAt;
    }
}
