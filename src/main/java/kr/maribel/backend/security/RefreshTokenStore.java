package kr.maribel.backend.security;

import java.time.Duration;
import kr.maribel.backend.domain.DomainEnums.RefreshTokenOwnerType;

public interface RefreshTokenStore {

    IssuedRefreshToken issue(RefreshTokenSubject subject, Duration ttl);

    IssuedRefreshToken issueForSession(String sessionId, RefreshTokenSubject subject, Duration ttl);

    ConsumedRefreshToken consume(String refreshToken);

    void revoke(String refreshToken);

    void revokeSession(String sessionId);

    void revokeOwner(RefreshTokenOwnerType ownerType, Long ownerId);
}
