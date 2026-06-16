package kr.maribel.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import kr.maribel.backend.domain.DomainEnums.TermsType;

@Entity
@Table(
        name = "terms",
        indexes = @Index(name = "idx_terms_type_published", columnList = "type,published_at"),
        uniqueConstraints = @UniqueConstraint(name = "uk_terms_type_version", columnNames = {"type", "version"})
)
public class TermsDocument extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TermsType type;

    @Column(nullable = false, length = 40)
    private String version;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    protected TermsDocument() {
    }

    public TermsDocument(TermsType type, String version, String content, Instant publishedAt) {
        this.type = type;
        this.version = version;
        this.content = content;
        this.publishedAt = publishedAt;
    }

    public Long getId() {
        return id;
    }

    public TermsType getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getContent() {
        return content;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
