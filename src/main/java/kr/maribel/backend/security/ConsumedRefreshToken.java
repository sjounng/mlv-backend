package kr.maribel.backend.security;

import java.time.Instant;

public record ConsumedRefreshToken(
        String sessionId,
        RefreshTokenSubject subject,
        Instant expiresAt
) {
}
