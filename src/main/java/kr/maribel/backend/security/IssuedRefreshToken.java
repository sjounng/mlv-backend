package kr.maribel.backend.security;

import java.time.Instant;

public record IssuedRefreshToken(
        String value,
        String tokenHash,
        String sessionId,
        Instant expiresAt
) {
}
