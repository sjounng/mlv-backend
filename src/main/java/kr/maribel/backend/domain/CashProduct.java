package kr.maribel.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * 캐시 충전 상품(패키지). 원화(KRW)로 결제해 캐시를 충전하는 상품. (07-22 웹상점 개편)
 *  - 본문 상세 소개는 모든 캐시 상품 페이지에 공통으로 표시되므로 SiteSetting 으로 별도 관리한다.
 */
@Entity
@Table(name = "cash_products", indexes = @Index(name = "idx_cash_products_active_sort", columnList = "active,sort_order"))
public class CashProduct extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    // 결제 금액 (원)
    @Column(name = "price_krw", nullable = false)
    private long priceKrw;

    // 지급 캐시량
    @Column(name = "cash_amount", nullable = false)
    private long cashAmount;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    protected CashProduct() {
    }

    public CashProduct(String name, long priceKrw, long cashAmount, String iconUrl, int sortOrder, boolean active) {
        this.name = name;
        this.priceKrw = priceKrw;
        this.cashAmount = cashAmount;
        this.iconUrl = iconUrl;
        this.sortOrder = sortOrder;
        this.active = active;
    }

    public void update(String name, long priceKrw, long cashAmount, String iconUrl, int sortOrder, boolean active) {
        this.name = name;
        this.priceKrw = priceKrw;
        this.cashAmount = cashAmount;
        this.iconUrl = iconUrl;
        this.sortOrder = sortOrder;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getPriceKrw() {
        return priceKrw;
    }

    public long getCashAmount() {
        return cashAmount;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }
}
