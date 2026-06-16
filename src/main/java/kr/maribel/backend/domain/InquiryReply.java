package kr.maribel.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "contact_replies")
public class InquiryReply extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private ContactInquiry inquiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private AdminAccount admin;

    @Lob
    @Column(nullable = false)
    private String content;

    protected InquiryReply() {
    }

    public InquiryReply(ContactInquiry inquiry, AdminAccount admin, String content) {
        this.inquiry = inquiry;
        this.admin = admin;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public ContactInquiry getInquiry() {
        return inquiry;
    }

    public AdminAccount getAdmin() {
        return admin;
    }

    public String getContent() {
        return content;
    }
}
