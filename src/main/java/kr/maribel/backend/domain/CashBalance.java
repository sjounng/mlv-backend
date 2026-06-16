package kr.maribel.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "cash_balances", uniqueConstraints = @UniqueConstraint(name = "uk_cash_balances_user", columnNames = "user_id"))
public class CashBalance extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private long balance;

    protected CashBalance() {
    }

    public CashBalance(Member member) {
        this.member = member;
    }

    public void credit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        this.balance += amount;
    }

    public void debit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (balance < amount) {
            throw new IllegalStateException("insufficient cash balance");
        }
        this.balance -= amount;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public long getBalance() {
        return balance;
    }
}
