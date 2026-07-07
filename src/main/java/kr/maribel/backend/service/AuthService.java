package kr.maribel.backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import kr.maribel.backend.api.ApiDtos.AdminLoginRequest;
import kr.maribel.backend.api.ApiDtos.DevLoginRequest;
import kr.maribel.backend.api.ApiDtos.MicrosoftAuthorizeUrlResponse;
import kr.maribel.backend.api.ApiDtos.TokenResponse;
import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.config.MaribelProperties;
import kr.maribel.backend.domain.AdminAccount;
// MicrosoftIdentity 는 동일 패키지(kr.maribel.backend.service)라 별도 import 불필요
import kr.maribel.backend.domain.CashBalance;
import kr.maribel.backend.domain.DomainEnums.RefreshTokenOwnerType;
import kr.maribel.backend.domain.DomainEnums.UserStatus;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.repository.AdminAccountRepository;
import kr.maribel.backend.repository.CashBalanceRepository;
import kr.maribel.backend.repository.MemberRepository;
import kr.maribel.backend.security.AuthenticatedPrincipal;
import kr.maribel.backend.security.ConsumedRefreshToken;
import kr.maribel.backend.security.IssuedRefreshToken;
import kr.maribel.backend.security.JwtTokenService;
import kr.maribel.backend.security.RefreshTokenStore;
import kr.maribel.backend.security.RefreshTokenSubject;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final AdminAccountRepository adminAccountRepository;
    private final CashBalanceRepository cashBalanceRepository;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final MaribelProperties properties;

    public AuthService(MemberRepository memberRepository,
                       AdminAccountRepository adminAccountRepository,
                       CashBalanceRepository cashBalanceRepository,
                       RefreshTokenStore refreshTokenStore,
                       JwtTokenService jwtTokenService,
                       PasswordEncoder passwordEncoder,
                       MaribelProperties properties) {
        this.memberRepository = memberRepository;
        this.adminAccountRepository = adminAccountRepository;
        this.cashBalanceRepository = cashBalanceRepository;
        this.refreshTokenStore = refreshTokenStore;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    public MicrosoftAuthorizeUrlResponse microsoftAuthorizeUrl(String state) {
        MaribelProperties.Microsoft microsoft = properties.getMicrosoft();
        if (!microsoft.isConfigured()) {
            return new MicrosoftAuthorizeUrlResponse(null, false);
        }
        String encodedRedirect = URLEncoder.encode(microsoft.getRedirectUri(), StandardCharsets.UTF_8);
        String encodedScopes = URLEncoder.encode(microsoft.getScopes(), StandardCharsets.UTF_8);
        String encodedState = URLEncoder.encode(state == null ? "" : state, StandardCharsets.UTF_8);
        String url = "https://login.microsoftonline.com/" + microsoft.getTenant() + "/oauth2/v2.0/authorize"
                + "?client_id=" + URLEncoder.encode(microsoft.getClientId(), StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&redirect_uri=" + encodedRedirect
                + "&scope=" + encodedScopes
                + "&state=" + encodedState;
        return new MicrosoftAuthorizeUrlResponse(url, true);
    }

    @Transactional
    public TokenResponse loginWithMicrosoft(MicrosoftIdentity identity) {
        Member member = memberRepository.findByMicrosoftSub(identity.microsoftSub())
                .map(existing -> {
                    existing.updateProfile(identity.minecraftUuid(), identity.minecraftUsername(), identity.email());
                    return existing;
                })
                // 신규 가입자는 약관 동의 전 상태로 생성한다.
                // 프론트가 agreedTermsAt == null 이면 동의 페이지로 보내고, POST /api/me/agree-terms 로 동의를 기록한다.
                .orElseGet(() -> memberRepository.save(
                        new Member(identity.microsoftSub(), identity.minecraftUuid(), identity.minecraftUsername(), identity.email())));

        cashBalanceRepository.findByMemberId(member.getId())
                .orElseGet(() -> cashBalanceRepository.save(new CashBalance(member)));

        return issueForMember(member);
    }

    @Transactional
    public TokenResponse devLogin(DevLoginRequest request) {
        if (!properties.isDevLoginEnabled()) {
            throw ApiException.notFound("DEV_LOGIN_DISABLED", "개발용 로그인이 비활성화되어 있습니다.");
        }
        Member member = memberRepository.findByMicrosoftSub(request.microsoftSub())
                .map(existing -> {
                    existing.updateProfile(request.minecraftUuid(), request.minecraftUsername(), request.email());
                    if (existing.getAgreedTermsAt() == null) {
                        existing.agreeRequiredTerms(request.marketingAgreed());
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Member created = new Member(request.microsoftSub(), request.minecraftUuid(), request.minecraftUsername(), request.email());
                    created.agreeRequiredTerms(request.marketingAgreed());
                    return memberRepository.save(created);
                });

        cashBalanceRepository.findByMemberId(member.getId())
                .orElseGet(() -> cashBalanceRepository.save(new CashBalance(member)));

        return issueForMember(member);
    }

    @Transactional
    public TokenResponse adminLogin(AdminLoginRequest request) {
        AdminAccount account = adminAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> ApiException.unauthorized("INVALID_ADMIN_LOGIN", "관리자 로그인 정보가 올바르지 않습니다."));
        if (!account.isActive() || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw ApiException.unauthorized("INVALID_ADMIN_LOGIN", "관리자 로그인 정보가 올바르지 않습니다.");
        }
        account.recordLogin();
        return issueForAdmin(account);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        ConsumedRefreshToken consumedToken = refreshTokenStore.consume(refreshToken);
        RefreshTokenSubject subject = consumedToken.subject();
        if (subject.ownerType() == RefreshTokenOwnerType.MEMBER) {
            Member member = memberRepository.findById(subject.ownerId())
                    .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "refresh token 소유자를 찾을 수 없습니다."));
            if (member.getStatus() != UserStatus.ACTIVE) {
                refreshTokenStore.revokeSession(consumedToken.sessionId());
                throw new ApiException(HttpStatus.FORBIDDEN, "USER_NOT_ACTIVE", "활성 회원만 로그인할 수 있습니다.");
            }
            return issueForMember(member, consumedToken.sessionId());
        }
        AdminAccount account = adminAccountRepository.findById(subject.ownerId())
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "refresh token 소유자를 찾을 수 없습니다."));
        if (!account.isActive()) {
            refreshTokenStore.revokeSession(consumedToken.sessionId());
            throw ApiException.unauthorized("ADMIN_NOT_ACTIVE", "비활성 관리자 계정입니다.");
        }
        return issueForAdmin(account, consumedToken.sessionId());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenStore.revoke(refreshToken);
    }

    private TokenResponse issueForMember(Member member) {
        return issueForMember(member, null);
    }

    private TokenResponse issueForMember(Member member, String sessionId) {
        RefreshTokenSubject subject = new RefreshTokenSubject(
                RefreshTokenOwnerType.MEMBER,
                member.getId(),
                member.getRole(),
                member.getMicrosoftSub(),
                member.getMinecraftUsername()
        );
        Duration refreshTtl = properties.getJwt().getMemberRefreshTokenTtl();
        IssuedRefreshToken refreshToken = sessionId == null
                ? refreshTokenStore.issue(subject, refreshTtl)
                : refreshTokenStore.issueForSession(sessionId, subject, refreshTtl);
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                member.getId(),
                null,
                member.getMicrosoftSub(),
                member.getMinecraftUsername(),
                member.getRole(),
                refreshToken.sessionId(),
                null
        );
        return new TokenResponse(
                jwtTokenService.createAccessToken(principal),
                refreshToken.value(),
                "Bearer",
                properties.getJwt().getAccessTokenTtl().toSeconds(),
                refreshTtl.toSeconds()
        );
    }

    private TokenResponse issueForAdmin(AdminAccount account) {
        return issueForAdmin(account, null);
    }

    private TokenResponse issueForAdmin(AdminAccount account, String sessionId) {
        RefreshTokenSubject subject = new RefreshTokenSubject(
                RefreshTokenOwnerType.ADMIN,
                account.getId(),
                account.getRole(),
                "admin:" + account.getUsername(),
                account.getUsername()
        );
        Duration refreshTtl = properties.getJwt().getAdminRefreshTokenTtl();
        IssuedRefreshToken refreshToken = sessionId == null
                ? refreshTokenStore.issue(subject, refreshTtl)
                : refreshTokenStore.issueForSession(sessionId, subject, refreshTtl);
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                null,
                account.getId(),
                "admin:" + account.getUsername(),
                account.getUsername(),
                account.getRole(),
                refreshToken.sessionId(),
                null
        );
        return new TokenResponse(
                jwtTokenService.createAccessToken(principal),
                refreshToken.value(),
                "Bearer",
                properties.getJwt().getAccessTokenTtl().toSeconds(),
                refreshTtl.toSeconds()
        );
    }

    @Transactional
    public void logoutAll(AuthenticatedPrincipal principal) {
        if (principal.adminId() != null) {
            refreshTokenStore.revokeOwner(RefreshTokenOwnerType.ADMIN, principal.adminId());
            return;
        }
        refreshTokenStore.revokeOwner(RefreshTokenOwnerType.MEMBER, principal.memberId());
    }
}
