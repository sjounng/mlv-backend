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
import kr.maribel.backend.domain.DomainEnums.ContactCategory;
import kr.maribel.backend.domain.DomainEnums.ContactStatus;

@Entity
@Table(name = "contact_inquiries", indexes = @Index(name = "idx_contact_inquiries_user_created", columnList = "user_id,created_at"))
public class ContactInquiry extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContactCategory category;

    @Column(nullable = false, length = 160)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContactStatus status = ContactStatus.OPEN;

    protected ContactInquiry() {
    }

    public ContactInquiry(Member member, ContactCategory category, String title, String content, String attachmentUrl) {
        this.member = member;
        this.category = category;
        this.title = title;
        this.content = content;
        this.attachmentUrl = attachmentUrl;
    }

    public void markAnswered() {
        this.status = ContactStatus.ANSWERED;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public ContactCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public ContactStatus getStatus() {
        return status;
    }
}
