package kr.maribel.backend.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.domain.DomainEnums.RefreshTokenOwnerType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private static final Duration USED_TOKEN_TTL = Duration.ofDays(1);

    private final RefreshTokenCodec refreshTokenCodec;
    private final ConcurrentMap<String, StoredRefreshToken> refreshTokens = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UsedRefreshToken> usedRefreshTokens = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, StoredSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> ownerSessions = new ConcurrentHashMap<>();

    public InMemoryRefreshTokenStore(RefreshTokenCodec refreshTokenCodec) {
        this.refreshTokenCodec = refreshTokenCodec;
    }

    @Override
    public IssuedRefreshToken issue(RefreshTokenSubject subject, Duration ttl) {
        return issueForSession(UUID.randomUUID().toString(), subject, ttl);
    }

    @Override
    public IssuedRefreshToken issueForSession(String sessionId, RefreshTokenSubject subject, Duration ttl) {
        String refreshToken = refreshTokenCodec.newToken();
        String tokenHash = refreshTokenCodec.hash(refreshToken);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

        refreshTokens.put(tokenHash, new StoredRefreshToken(sessionId, subject, expiresAt));
        StoredSession existing = sessions.get(sessionId);
        Instant createdAt = existing == null ? now : existing.createdAt();
        sessions.put(sessionId, new StoredSession(subject, tokenHash, createdAt, now));
        ownerSessions.computeIfAbsent(ownerSessionsKey(subject.ownerType(), subject.ownerId()), ignored -> ConcurrentHashMap.newKeySet())
                .add(sessionId);

        return new IssuedRefreshToken(refreshToken, tokenHash, sessionId, expiresAt);
    }

    @Override
    public ConsumedRefreshToken consume(String refreshToken) {
        String tokenHash = refreshTokenCodec.hash(refreshToken);
        StoredRefreshToken storedToken = refreshTokens.remove(tokenHash);
        if (storedToken == null) {
            UsedRefreshToken usedToken = usedRefreshTokens.get(tokenHash);
            if (usedToken != null && usedToken.expiresAt().isAfter(Instant.now())) {
                revokeSession(usedToken.sessionId());
                throw ApiException.unauthorized("REFRESH_TOKEN_REUSED", "이미 사용된 refresh token입니다. 세션을 종료했습니다.");
            }
            throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "refresh token이 올바르지 않습니다.");
        }
        if (!storedToken.expiresAt().isAfter(Instant.now())) {
            revokeSession(storedToken.sessionId());
            throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "refresh token이 만료되었습니다.");
        }
        usedRefreshTokens.put(tokenHash, new UsedRefreshToken(storedToken.sessionId(), Instant.now().plus(USED_TOKEN_TTL)));
        return new ConsumedRefreshToken(storedToken.sessionId(), storedToken.subject(), storedToken.expiresAt());
    }

    @Override
    public void revoke(String refreshToken) {
        String tokenHash = refreshTokenCodec.hash(refreshToken);
        StoredRefreshToken storedToken = refreshTokens.remove(tokenHash);
        if (storedToken != null) {
            revokeSession(storedToken.sessionId());
        }
    }

    @Override
    public void revokeSession(String sessionId) {
        StoredSession session = sessions.remove(sessionId);
        if (session == null) {
            return;
        }
        refreshTokens.remove(session.currentRefreshTokenHash());
        Set<String> sessionsForOwner = ownerSessions.get(ownerSessionsKey(session.subject().ownerType(), session.subject().ownerId()));
        if (sessionsForOwner != null) {
            sessionsForOwner.remove(sessionId);
        }
    }

    @Override
    public void revokeOwner(RefreshTokenOwnerType ownerType, Long ownerId) {
        Set<String> sessionsForOwner = ownerSessions.remove(ownerSessionsKey(ownerType, ownerId));
        if (sessionsForOwner != null) {
            sessionsForOwner.forEach(this::revokeSession);
        }
    }

    private String ownerSessionsKey(RefreshTokenOwnerType ownerType, Long ownerId) {
        return ownerType.name() + ":" + ownerId;
    }

    private record StoredRefreshToken(
            String sessionId,
            RefreshTokenSubject subject,
            Instant expiresAt
    ) {
    }

    private record UsedRefreshToken(
            String sessionId,
            Instant expiresAt
    ) {
    }

    private record StoredSession(
            RefreshTokenSubject subject,
            String currentRefreshTokenHash,
            Instant createdAt,
            Instant lastUsedAt
    ) {
    }
}
