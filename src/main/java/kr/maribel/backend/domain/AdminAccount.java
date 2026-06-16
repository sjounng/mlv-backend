package kr.maribel.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import kr.maribel.backend.domain.DomainEnums.Role;

@Entity
@Table(name = "admin_users", uniqueConstraints = @UniqueConstraint(name = "uk_admin_users_username", columnNames = "username"))
public class AdminAccount extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(nullable = false)
    private boolean active = true;

    protected AdminAccount() {
    }

    public AdminAccount(String username, String passwordHash, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public void recordLogin() {
        this.lastLoginAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public boolean isActive() {
        return active;
    }
}
