package kr.maribel.backend.security;

import kr.maribel.backend.domain.DomainEnums.Role;

public record AuthenticatedPrincipal(
        Long memberId,
        Long adminId,
        String subject,
        String displayName,
        Role role,
        String sessionId,
        String accessTokenId
) {
    public boolean isAdmin() {
        return role == Role.OPERATOR || role == Role.SUPER_ADMIN;
    }
}
