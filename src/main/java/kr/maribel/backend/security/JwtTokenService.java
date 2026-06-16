package kr.maribel.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.config.MaribelProperties;
import kr.maribel.backend.domain.DomainEnums.Role;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class JwtTokenService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final MaribelProperties properties;
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder decoder = Base64.getUrlDecoder();

    public JwtTokenService(ObjectMapper objectMapper, MaribelProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String createAccessToken(AuthenticatedPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getJwt().getAccessTokenTtl());

        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("typ", "access");
        payload.put("iss", properties.getJwt().getIssuer());
        payload.put("sub", principal.subject());
        payload.put("name", principal.displayName());
        payload.put("role", principal.role().name());
        payload.put("sid", principal.sessionId());
        payload.put("jti", UUID.randomUUID().toString());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());
        if (principal.memberId() != null) {
            payload.put("uid", principal.memberId());
        }
        if (principal.adminId() != null) {
            payload.put("aid", principal.adminId());
        }

        try {
            String encodedHeader = encoder.encodeToString(objectMapper.writeValueAsBytes(header));
            String encodedPayload = encoder.encodeToString(objectMapper.writeValueAsBytes(payload));
            String signingInput = encodedHeader + "." + encodedPayload;
            return signingInput + "." + sign(signingInput);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to create jwt", exception);
        }
    }

    public AuthenticatedPrincipal parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw ApiException.unauthorized("INVALID_TOKEN", "인증 토큰 형식이 올바르지 않습니다.");
            }

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = sign(signingInput);
            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw ApiException.unauthorized("INVALID_TOKEN", "인증 토큰 서명이 올바르지 않습니다.");
            }

            Map<String, Object> payload = objectMapper.readValue(decoder.decode(parts[1]), MAP_TYPE);
            if (!"access".equals(payload.get("typ"))) {
                throw ApiException.unauthorized("INVALID_TOKEN", "인증 토큰 유형이 올바르지 않습니다.");
            }
            if (!properties.getJwt().getIssuer().equals(payload.get("iss"))) {
                throw ApiException.unauthorized("INVALID_TOKEN", "인증 토큰 발급자가 올바르지 않습니다.");
            }
            long expiresAt = ((Number) payload.get("exp")).longValue();
            if (Instant.ofEpochSecond(expiresAt).isBefore(Instant.now())) {
                throw ApiException.unauthorized("TOKEN_EXPIRED", "인증 토큰이 만료되었습니다.");
            }

            Role role = Role.valueOf(String.valueOf(payload.get("role")));
            Long memberId = numberToLong(payload.get("uid"));
            Long adminId = numberToLong(payload.get("aid"));
            return new AuthenticatedPrincipal(
                    memberId,
                    adminId,
                    String.valueOf(payload.get("sub")),
                    String.valueOf(payload.get("name")),
                    role,
                    String.valueOf(payload.get("sid")),
                    String.valueOf(payload.get("jti"))
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ApiException.unauthorized("INVALID_TOKEN", "인증 토큰을 해석할 수 없습니다.");
        }
    }

    private String sign(String signingInput) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return encoder.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    }

    private Long numberToLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
