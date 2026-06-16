package kr.maribel.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "mail_templates", uniqueConstraints = @UniqueConstraint(name = "uk_mail_templates_code", columnNames = "mail_code"))
public class MailTemplate extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mail_code", nullable = false, length = 80)
    private String mailCode;

    @Column(nullable = false, length = 160)
    private String subject;

    @Lob
    @Column(nullable = false)
    private String content;

    @Lob
    @Column(name = "rewards_json", nullable = false)
    private String rewardsJson;

    protected MailTemplate() {
    }

    public MailTemplate(String mailCode, String subject, String content, String rewardsJson) {
        this.mailCode = mailCode;
        this.subject = subject;
        this.content = content;
        this.rewardsJson = rewardsJson;
    }

    public void update(String subject, String content, String rewardsJson) {
        this.subject = subject;
        this.content = content;
        this.rewardsJson = rewardsJson;
    }

    public Long getId() {
        return id;
    }

    public String getMailCode() {
        return mailCode;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public String getRewardsJson() {
        return rewardsJson;
    }
}
