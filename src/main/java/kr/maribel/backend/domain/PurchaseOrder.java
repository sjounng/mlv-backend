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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.maribel.backend.domain.DomainEnums.PurchaseStatus;

@Entity
@Table(
        name = "purchase_orders",
        indexes = @Index(name = "idx_purchase_orders_user_created", columnList = "user_id,created_at"),
        uniqueConstraints = @UniqueConstraint(name = "uk_purchase_orders_order_number", columnNames = "order_number")
)
public class PurchaseOrder extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 80)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "total_price", nullable = false)
    private long totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PurchaseStatus status = PurchaseStatus.MAIL_PENDING;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_mail_id")
    private OutboundMail outboundMail;

    protected PurchaseOrder() {
    }

    public PurchaseOrder(String orderNumber, Member member, Product product, int quantity, long totalPrice) {
        this.orderNumber = orderNumber;
        this.member = member;
        this.product = product;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    public void attachMail(OutboundMail outboundMail) {
        this.outboundMail = outboundMail;
        this.status = PurchaseStatus.MAIL_PENDING;
    }

    public void complete() {
        this.status = PurchaseStatus.COMPLETED;
    }

    public void markMailFailed() {
        this.status = PurchaseStatus.MAIL_FAILED;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Member getMember() {
        return member;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getTotalPrice() {
        return totalPrice;
    }

    public PurchaseStatus getStatus() {
        return status;
    }

    public OutboundMail getOutboundMail() {
        return outboundMail;
    }
}
