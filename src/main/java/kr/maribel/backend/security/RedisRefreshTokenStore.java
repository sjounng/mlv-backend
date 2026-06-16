package kr.maribel.backend.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.domain.DomainEnums.RefreshTokenOwnerType;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@Profile("!test")
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "maribel:auth:";
    private static final Duration USED_TOKEN_TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RefreshTokenCodec refreshTokenCodec;

    public RedisRefreshTokenStore(StringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper,
                                  RefreshTokenCodec refreshTokenCodec) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
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

        StoredRefreshToken storedToken = new StoredRefreshToken(sessionId, subject, expiresAt.toString());
        StoredSession storedSession = readSession(sessionId);
        Instant createdAt = storedSession == null ? now : Instant.parse(storedSession.createdAt());
        StoredSession nextSession = new StoredSession(subject, tokenHash, createdAt.toString(), now.toString());

        write(refreshTokenKey(tokenHash), storedToken, ttl);
        write(sessionKey(sessionId), nextSession, ttl);
        redisTemplate.opsForSet().add(ownerSessionsKey(subject.ownerType(), subject.ownerId()), sessionId);
        redisTemplate.expire(ownerSessionsKey(subject.ownerType(), subject.ownerId()), ttl);

        return new IssuedRefreshToken(refreshToken, tokenHash, sessionId, expiresAt);
    }

    @Override
    public ConsumedRefreshToken consume(String refreshToken) {
        String tokenHash = refreshTokenCodec.hash(refreshToken);
        String tokenJson = redisTemplate.opsForValue().getAndDelete(refreshTokenKey(tokenHash));
        if (tokenJson == null) {
            String replayedSessionId = redisTemplate.opsForValue().get(usedRefreshTokenKey(tokenHash));
            if (replayedSessionId != null) {
                revokeSession(replayedSessionId);
                throw ApiException.unauthorized("REFRESH_TOKEN_REUSED", "이미 사용된 refresh token입니다. 세션을 종료했습니다.");
            }
            throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "refresh token이 올바르지 않습니다.");
        }

        StoredRefreshToken storedToken = read(tokenJson, StoredRefreshToken.class);
        Instant expiresAt = Instant.parse(storedToken.expiresAt());
        if (!expiresAt.isAfter(Instant.now())) {
            revokeSession(storedToken.sessionId());
            throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "refresh token이 만료되었습니다.");
        }

        redisTemplate.opsForValue().set(usedRefreshTokenKey(tokenHash), storedToken.sessionId(), USED_TOKEN_TTL);
        return new ConsumedRefreshToken(storedToken.sessionId(), storedToken.subject(), expiresAt);
    }

    @Override
    public void revoke(String refreshToken) {
        String tokenHash = refreshTokenCodec.hash(refreshToken);
        String tokenJson = redisTemplate.opsForValue().getAndDelete(refreshTokenKey(tokenHash));
        if (tokenJson == null) {
            return;
        }
        StoredRefreshToken storedToken = read(tokenJson, StoredRefreshToken.class);
        revokeSession(storedToken.sessionId());
    }

    @Override
    public void revokeSession(String sessionId) {
        StoredSession storedSession = readSession(sessionId);
        redisTemplate.delete(sessionKey(sessionId));
        if (storedSession == null) {
            return;
        }
        redisTemplate.delete(refreshTokenKey(storedSession.currentRefreshTokenHash()));
        redisTemplate.opsForSet().remove(ownerSessionsKey(storedSession.subject().ownerType(), storedSession.subject().ownerId()), sessionId);
    }

    @Override
    public void revokeOwner(RefreshTokenOwnerType ownerType, Long ownerId) {
        String key = ownerSessionsKey(ownerType, ownerId);
        Set<String> sessionIds = redisTemplate.opsForSet().members(key);
        if (sessionIds != null) {
            sessionIds.forEach(this::revokeSession);
        }
        redisTemplate.delete(key);
    }

    private StoredSession readSession(String sessionId) {
        String json = redisTemplate.opsForValue().get(sessionKey(sessionId));
        if (json == null) {
            return null;
        }
        return read(json, StoredSession.class);
    }

    private void write(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to write refresh token session", exception);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to read refresh token session", exception);
        }
    }

    private String refreshTokenKey(String tokenHash) {
        return KEY_PREFIX + "rt:" + tokenHash;
    }

    private String usedRefreshTokenKey(String tokenHash) {
        return KEY_PREFIX + "used-rt:" + tokenHash;
    }

    private String sessionKey(String sessionId) {
        return KEY_PREFIX + "session:" + sessionId;
    }

    private String ownerSessionsKey(RefreshTokenOwnerType ownerType, Long ownerId) {
        return KEY_PREFIX + "owner:" + ownerType.name() + ":" + ownerId + ":sessions";
    }

    private record StoredRefreshToken(
            String sessionId,
            RefreshTokenSubject subject,
            String expiresAt
    ) {
    }

    private record StoredSession(
            RefreshTokenSubject subject,
            String currentRefreshTokenHash,
            String createdAt,
            String lastUsedAt
    ) {
    }
}
