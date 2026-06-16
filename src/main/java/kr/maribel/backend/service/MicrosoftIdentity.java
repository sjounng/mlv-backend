package kr.maribel.backend.service;

/**
 * Microsoft OAuth → Xbox Live → XSTS → Minecraft Profile 체인으로 확보한
 * 회원 식별 정보.
 */
public record MicrosoftIdentity(
        String microsoftSub,
        String minecraftUuid,
        String minecraftUsername,
        String email
) {
}
