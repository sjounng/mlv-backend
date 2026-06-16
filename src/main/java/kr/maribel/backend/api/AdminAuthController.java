package kr.maribel.backend.api;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.maribel.backend.api.ApiDtos.AdminLoginRequest;
import kr.maribel.backend.api.ApiDtos.TokenResponse;
import kr.maribel.backend.security.RefreshTokenCookieService;
import kr.maribel.backend.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@Tag(name = "Admin Auth")
public class AdminAuthController {

    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public AdminAuthController(AuthService authService, RefreshTokenCookieService refreshTokenCookieService) {
        this.authService = authService;
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    @PostMapping("/login")
    @Operation(summary = "관리자 로그인")
    TokenResponse login(@Valid @RequestBody AdminLoginRequest request, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.adminLogin(request);
        refreshTokenCookieService.write(response, tokenResponse.refreshToken(), tokenResponse.refreshExpiresInSeconds());
        return tokenResponse;
    }
}
