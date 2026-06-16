package kr.maribel.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_category_active", columnList = "category_id,active"),
        @Index(name = "idx_products_price", columnList = "price")
})
public class Product extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private long price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mail_template_id", nullable = false)
    private MailTemplate mailTemplate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "recommended", nullable = false)
    private boolean recommended;

    @Column(name = "new_badge", nullable = false)
    private boolean newBadge;

    @Column(name = "price_updated_at")
    private Instant priceUpdatedAt;

    protected Product() {
    }

    public Product(String name, String description, long price, String imageUrl, Category category, MailTemplate mailTemplate) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
        this.mailTemplate = mailTemplate;
        this.priceUpdatedAt = Instant.now();
    }

    public void update(String name, String description, long price, String imageUrl, Category category,
                       MailTemplate mailTemplate, boolean active, Integer stockQuantity,
                       boolean recommended, boolean newBadge) {
        this.name = name;
        this.description = description;
        if (this.price != price) {
            this.priceUpdatedAt = Instant.now();
        }
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
        this.mailTemplate = mailTemplate;
        this.active = active;
        this.stockQuantity = stockQuantity;
        this.recommended = recommended;
        this.newBadge = newBadge;
    }

    public void decreaseStock(int quantity) {
        if (stockQuantity == null) {
            return;
        }
        if (stockQuantity < quantity) {
            throw new IllegalStateException("insufficient stock");
        }
        stockQuantity -= quantity;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Category getCategory() {
        return category;
    }

    public MailTemplate getMailTemplate() {
        return mailTemplate;
    }

    public boolean isActive() {
        return active;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public boolean isNewBadge() {
        return newBadge;
    }
}
