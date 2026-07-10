package kr.maribel.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import kr.maribel.backend.domain.DomainEnums.Role;
import kr.maribel.backend.domain.DomainEnums.UserStatus;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_minecraft_username", columnList = "minecraft_username"),
                @Index(name = "idx_users_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_microsoft_sub", columnNames = "microsoft_sub"),
                @UniqueConstraint(name = "uk_users_minecraft_uuid", columnNames = "minecraft_uuid")
        }
)
public class Member extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "microsoft_sub", nullable = false, length = 120)
    private String microsoftSub;

    @Column(name = "minecraft_uuid", nullable = false, length = 36)
    private String minecraftUuid;

    @Column(name = "minecraft_username", nullable = false, length = 32)
    private String minecraftUsername;

    @Column(length = 254)
    private String email;

    @Column(name = "agreed_terms_at")
    private Instant agreedTermsAt;

    @Column(name = "agreed_privacy_at")
    private Instant agreedPrivacyAt;

    @Column(name = "marketing_agreed")
    private boolean marketingAgreed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role = Role.USER;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    // 누적 경고 횟수 (관리자 경고 부여 시스템은 추후 구현, 표시용 필드 선반영)
    @Column(name = "warning_count", nullable = false)
    private int warningCount = 0;

    protected Member() {
    }

    public Member(String microsoftSub, String minecraftUuid, String minecraftUsername, String email) {
        this.microsoftSub = microsoftSub;
        this.minecraftUuid = minecraftUuid;
        this.minecraftUsername = minecraftUsername;
        this.email = email;
    }

    public void updateProfile(String minecraftUuid, String minecraftUsername, String email) {
        this.minecraftUuid = minecraftUuid;
        this.minecraftUsername = minecraftUsername;
        this.email = email;
    }

    public void agreeRequiredTerms(boolean marketingAgreed) {
        Instant now = Instant.now();
        this.agreedTermsAt = now;
        this.agreedPrivacyAt = now;
        this.marketingAgreed = marketingAgreed;
    }

    public void suspend() {
        if (this.status == UserStatus.WITHDRAWN) {
            throw new IllegalStateException("withdrawn member cannot be suspended");
        }
        this.status = UserStatus.SUSPENDED;
    }

    public void reactivate() {
        if (this.status == UserStatus.WITHDRAWN) {
            throw new IllegalStateException("withdrawn member cannot be reactivated");
        }
        this.status = UserStatus.ACTIVE;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        this.withdrawnAt = Instant.now();
        this.microsoftSub = "withdrawn:" + id + ":" + withdrawnAt.toEpochMilli();
        this.minecraftUuid = UUID.nameUUIDFromBytes(("withdrawn:" + id + ":" + withdrawnAt).getBytes(StandardCharsets.UTF_8)).toString();
        this.minecraftUsername = "withdrawn";
        this.email = null;
    }

    public Long getId() {
        return id;
    }

    public String getMicrosoftSub() {
        return microsoftSub;
    }

    public String getMinecraftUuid() {
        return minecraftUuid;
    }

    public String getMinecraftUsername() {
        return minecraftUsername;
    }

    public String getEmail() {
        return email;
    }

    public Instant getAgreedTermsAt() {
        return agreedTermsAt;
    }

    public Instant getAgreedPrivacyAt() {
        return agreedPrivacyAt;
    }

    public boolean isMarketingAgreed() {
        return marketingAgreed;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Role getRole() {
        return role;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }

    public int getWarningCount() {
        return warningCount;
    }

    /** 유효 경고 수와 동기화 (경고 부여/취소 시 호출) */
    public void setWarningCount(int warningCount) {
        this.warningCount = Math.max(0, warningCount);
    }
}
