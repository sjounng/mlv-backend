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
import kr.maribel.backend.domain.DomainEnums.CashTransactionType;

@Entity
@Table(name = "cash_transactions", indexes = @Index(name = "idx_cash_transactions_user_created", columnList = "user_id,created_at"))
public class CashTransaction extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CashTransactionType type;

    @Column(nullable = false)
    private long amount;

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

    @Column(name = "ref_id", length = 120)
    private String refId;

    @Column(length = 300)
    private String memo;

    protected CashTransaction() {
    }

    public CashTransaction(Member member, CashTransactionType type, long amount, long balanceAfter, String refId, String memo) {
        this.member = member;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.refId = refId;
        this.memo = memo;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public CashTransactionType getType() {
        return type;
    }

    public long getAmount() {
        return amount;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public String getRefId() {
        return refId;
    }

    public String getMemo() {
        return memo;
    }
}
