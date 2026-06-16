package kr.maribel.backend.security;

import kr.maribel.backend.domain.DomainEnums.RefreshTokenOwnerType;
import kr.maribel.backend.domain.DomainEnums.Role;

public record RefreshTokenSubject(
        RefreshTokenOwnerType ownerType,
        Long ownerId,
        Role role,
        String subject,
        String displayName
) {
}
