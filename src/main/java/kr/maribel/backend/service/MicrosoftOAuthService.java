package kr.maribel.backend.service;

import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.config.MaribelProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Base64;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Microsoft OAuth authorization code 를 받아
 * Xbox Live → XSTS → Minecraft Services 체인을 거쳐 마인크래프트 프로필을 확보한다. (FR-A2)
 */
@Service
public class MicrosoftOAuthService {

    private static final Logger log = LoggerFactory.getLogger(MicrosoftOAuthService.class);

    private static final String XBL_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MC_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    private final MaribelProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public MicrosoftOAuthService(MaribelProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public MicrosoftIdentity authenticate(String code) {
        MaribelProperties.Microsoft microsoft = properties.getMicrosoft();
        if (!microsoft.isConfigured()) {
            throw ApiException.badRequest("MICROSOFT_NOT_CONFIGURED", "Microsoft OAuth가 설정되지 않았습니다.");
        }

        MsToken msToken = exchangeCode(code, microsoft);
        IdTokenClaims claims = parseIdToken(msToken.idToken());

        String xblToken = xboxLiveAuthenticate(msToken.accessToken());
        XstsResult xsts = xstsAuthorize(xblToken);
        String mcAccessToken = minecraftLogin(xsts.userHash(), xsts.token());
        MinecraftProfile profile = minecraftProfile(mcAccessToken);

        String microsoftSub = StringUtils.hasText(claims.sub()) ? claims.sub() : "mc:" + profile.id();
        return new MicrosoftIdentity(
                microsoftSub,
                formatUuid(profile.id()),
                profile.name(),
                claims.email()
        );
    }

    private MsToken exchangeCode(String code, MaribelProperties.Microsoft microsoft) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", microsoft.getClientId());
        form.add("client_secret", microsoft.getClientSecret());
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", microsoft.getRedirectUri());
        form.add("scope", microsoft.getScopes());

        String tokenUrl = "https://login.microsoftonline.com/" + microsoft.getTenant() + "/oauth2/v2.0/token";
        JsonNode node = postForm(tokenUrl, form, "MS_TOKEN_EXCHANGE_FAILED", "Microsoft 토큰 교환에 실패했습니다.");
        String accessToken = node.path("access_token").asString();
        if (!StringUtils.hasText(accessToken)) {
            throw ApiException.badRequest("MS_TOKEN_EXCHANGE_FAILED", "Microsoft access token을 받지 못했습니다.");
        }
        return new MsToken(accessToken, node.path("id_token").asString());
    }

    private IdTokenClaims parseIdToken(String idToken) {
        if (!StringUtils.hasText(idToken)) {
            return new IdTokenClaims(null, null);
        }
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                return new IdTokenClaims(null, null);
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode node = objectMapper.readTree(payload);
            String sub = node.path("sub").asString(null);
            String email = node.hasNonNull("email") ? node.path("email").asString() : null;
            return new IdTokenClaims(sub, email);
        } catch (Exception exception) {
            log.warn("failed to parse Microsoft id_token", exception);
            return new IdTokenClaims(null, null);
        }
    }

    private String xboxLiveAuthenticate(String msAccessToken) {
        String body = """
                {"Properties":{"AuthMethod":"RPS","SiteName":"user.auth.xboxlive.com","RpsTicket":"d=%s"},\
                "RelyingParty":"http://auth.xboxlive.com","TokenType":"JWT"}""".formatted(msAccessToken);
        JsonNode node = postJson(XBL_AUTH_URL, body, "XBL_AUTH_FAILED", "Xbox Live 인증에 실패했습니다.");
        String token = node.path("Token").asString();
        if (!StringUtils.hasText(token)) {
            throw ApiException.badRequest("XBL_AUTH_FAILED", "Xbox Live 토큰을 받지 못했습니다.");
        }
        return token;
    }

    private XstsResult xstsAuthorize(String xblToken) {
        String body = """
                {"Properties":{"SandboxId":"RETAIL","UserTokens":["%s"]},\
                "RelyingParty":"rp://api.minecraftservices.com/","TokenType":"JWT"}""".formatted(xblToken);
        JsonNode node;
        try {
            node = postJson(XSTS_AUTH_URL, body, "XSTS_AUTH_FAILED", "XSTS 인증에 실패했습니다.");
        } catch (ApiException exception) {
            // XErr 코드(미성년 계정/Xbox 미가입 등)를 좀 더 친절히 안내할 수 있으나 우선 일반 처리한다.
            throw exception;
        }
        String token = node.path("Token").asString();
        String userHash = node.path("DisplayClaims").path("xui").path(0).path("uhs").asString();
        if (!StringUtils.hasText(token) || !StringUtils.hasText(userHash)) {
            throw ApiException.badRequest("XSTS_AUTH_FAILED", "XSTS 토큰을 받지 못했습니다.");
        }
        return new XstsResult(token, userHash);
    }

    private String minecraftLogin(String userHash, String xstsToken) {
        String body = "{\"identityToken\":\"XBL3.0 x=" + userHash + ";" + xstsToken + "\"}";
        JsonNode node = postJson(MC_LOGIN_URL, body, "MC_LOGIN_FAILED", "Minecraft 인증에 실패했습니다.");
        String accessToken = node.path("access_token").asString();
        if (!StringUtils.hasText(accessToken)) {
            throw ApiException.badRequest("MC_LOGIN_FAILED", "Minecraft access token을 받지 못했습니다.");
        }
        return accessToken;
    }

    private MinecraftProfile minecraftProfile(String mcAccessToken) {
        try {
            String response = restClient.get()
                    .uri(MC_PROFILE_URL)
                    .header("Authorization", "Bearer " + mcAccessToken)
                    .retrieve()
                    .body(String.class);
            JsonNode node = objectMapper.readTree(response);
            String id = node.path("id").asString();
            String name = node.path("name").asString();
            if (!StringUtils.hasText(id) || !StringUtils.hasText(name)) {
                throw ApiException.badRequest("MC_PROFILE_NOT_FOUND", "마인크래프트 정품 프로필을 찾을 수 없습니다.");
            }
            return new MinecraftProfile(id, name);
        } catch (RestClientResponseException exception) {
            // 자바 에디션 미보유(정품 아님) 시 404 가 반환된다.
            throw new ApiException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "MC_PROFILE_NOT_FOUND",
                    "마인크래프트 자바 에디션 정품 계정이 아닙니다.");
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("failed to fetch minecraft profile", exception);
        }
    }

    private JsonNode postForm(String url, MultiValueMap<String, String> form, String errorCode, String errorMessage) {
        try {
            String response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(response);
        } catch (RestClientResponseException exception) {
            log.warn("{} status={} body={}", errorCode, exception.getStatusCode(), exception.getResponseBodyAsString());
            throw ApiException.badRequest(errorCode, errorMessage);
        } catch (Exception exception) {
            throw new IllegalStateException("failed call: " + url, exception);
        }
    }

    private JsonNode postJson(String url, String jsonBody, String errorCode, String errorMessage) {
        try {
            String response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(response);
        } catch (RestClientResponseException exception) {
            log.warn("{} status={} body={}", errorCode, exception.getStatusCode(), exception.getResponseBodyAsString());
            throw ApiException.badRequest(errorCode, errorMessage);
        } catch (Exception exception) {
            throw new IllegalStateException("failed call: " + url, exception);
        }
    }

    private String formatUuid(String undashed) {
        if (undashed.length() != 32) {
            return undashed;
        }
        return undashed.substring(0, 8) + "-"
                + undashed.substring(8, 12) + "-"
                + undashed.substring(12, 16) + "-"
                + undashed.substring(16, 20) + "-"
                + undashed.substring(20);
    }

    private record MsToken(String accessToken, String idToken) {
    }

    private record IdTokenClaims(String sub, String email) {
    }

    private record XstsResult(String token, String userHash) {
    }

    private record MinecraftProfile(String id, String name) {
    }
}
