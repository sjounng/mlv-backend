package kr.maribel.backend.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.maribel.backend.api.ApiDtos.DevLoginRequest;
import kr.maribel.backend.api.ApiDtos.LogoutRequest;
import kr.maribel.backend.api.ApiDtos.MicrosoftAuthorizeUrlResponse;
import kr.maribel.backend.api.ApiDtos.RefreshRequest;
import kr.maribel.backend.api.ApiDtos.TokenResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import kr.maribel.backend.config.MaribelProperties;
import kr.maribel.backend.config.OpenApiConfig;
import kr.maribel.backend.security.AuthenticatedPrincipal;
import kr.maribel.backend.security.RefreshTokenCookieService;
import kr.maribel.backend.service.AuthService;
import kr.maribel.backend.service.MicrosoftIdentity;
import kr.maribel.backend.service.MicrosoftOAuthService;
import org.springframework.util.StringUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final MicrosoftOAuthService microsoftOAuthService;
    private final MaribelProperties properties;

    public AuthController(AuthService authService,
                          RefreshTokenCookieService refreshTokenCookieService,
                          MicrosoftOAuthService microsoftOAuthService,
                          MaribelProperties properties) {
        this.authService = authService;
        this.refreshTokenCookieService = refreshTokenCookieService;
        this.microsoftOAuthService = microsoftOAuthService;
        this.properties = properties;
    }

    @GetMapping("/microsoft/authorize-url")
    @Operation(summary = "Microsoft OAuth authorize URL 생성")
    MicrosoftAuthorizeUrlResponse microsoftAuthorizeUrl(@RequestParam(required = false) String state) {
        return authService.microsoftAuthorizeUrl(state);
    }

    @GetMapping("/microsoft/callback")
    @Operation(summary = "Microsoft OAuth 콜백 — 토큰 교환 후 프론트엔드로 리다이렉트")
    void microsoftCallback(@RequestParam(required = false) String code,
                           @RequestParam(required = false) String state,
                           @RequestParam(required = false) String error,
                           @RequestParam(name = "error_description", required = false) String errorDescription,
                           HttpServletResponse response) throws IOException {
        MaribelProperties.Microsoft microsoft = properties.getMicrosoft();

        if (StringUtils.hasText(error)) {
            response.sendRedirect(failureUrl(microsoft, error));
            return;
        }
        if (!StringUtils.hasText(code)) {
            response.sendRedirect(failureUrl(microsoft, "missing_code"));
            return;
        }

        try {
            MicrosoftIdentity identity = microsoftOAuthService.authenticate(code);
            TokenResponse tokenResponse = authService.loginWithMicrosoft(identity);
            refreshTokenCookieService.write(response, tokenResponse.refreshToken(), tokenResponse.refreshExpiresInSeconds());
            response.sendRedirect(successUrl(microsoft, state));
        } catch (ApiException exception) {
            response.sendRedirect(failureUrl(microsoft, exception.getCode()));
        }
    }

    private String successUrl(MaribelProperties.Microsoft microsoft, String state) {
        String base = microsoft.getSuccessRedirectUri();
        String url = base + (base.contains("?") ? "&" : "?") + "status=success";
        if (StringUtils.hasText(state)) {
            url += "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
        }
        return url;
    }

    private String failureUrl(MaribelProperties.Microsoft microsoft, String reason) {
        String base = microsoft.getFailureRedirectUri();
        return base + (base.contains("?") ? "&" : "?") + "error="
                + URLEncoder.encode(reason == null ? "login_failed" : reason, StandardCharsets.UTF_8);
    }

    @PostMapping("/dev-login")
    @Operation(summary = "로컬 개발용 회원 로그인")
    TokenResponse devLogin(@Valid @RequestBody DevLoginRequest request, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.devLogin(request);
        refreshTokenCookieService.write(response, tokenResponse.refreshToken(), tokenResponse.refreshExpiresInSeconds());
        return tokenResponse;
    }

    @PostMapping("/refresh")
    @Operation(summary = "Access token 재발급", security = @SecurityRequirement(name = OpenApiConfig.REFRESH_COOKIE))
    TokenResponse refresh(@Valid @RequestBody(required = false) RefreshRequest request,
                          HttpServletRequest servletRequest,
                          HttpServletResponse response) {
        TokenResponse tokenResponse = authService.refresh(resolveRefreshToken(request == null ? null : request.refreshToken(), servletRequest));
        refreshTokenCookieService.write(response, tokenResponse.refreshToken(), tokenResponse.refreshExpiresInSeconds());
        return tokenResponse;
    }

    @PostMapping("/logout")
    @Operation(summary = "Refresh token 폐기", security = @SecurityRequirement(name = OpenApiConfig.REFRESH_COOKIE))
    void logout(@Valid @RequestBody(required = false) LogoutRequest request,
                HttpServletRequest servletRequest,
                HttpServletResponse response) {
        authService.logout(resolveRefreshToken(request == null ? null : request.refreshToken(), servletRequest));
        refreshTokenCookieService.clear(response);
    }

    @PostMapping("/logout-all")
    @Operation(summary = "현재 계정의 모든 refresh session 폐기", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    void logoutAll(@AuthenticationPrincipal AuthenticatedPrincipal principal, HttpServletResponse response) {
        if (principal == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "인증이 필요합니다.");
        }
        authService.logoutAll(principal);
        refreshTokenCookieService.clear(response);
    }

    private String resolveRefreshToken(String bodyRefreshToken, HttpServletRequest request) {
        if (bodyRefreshToken != null && !bodyRefreshToken.isBlank()) {
            return bodyRefreshToken;
        }
        return refreshTokenCookieService.read(request)
                .orElseThrow(() -> ApiException.unauthorized("MISSING_REFRESH_TOKEN", "refresh token이 필요합니다."));
    }
}
