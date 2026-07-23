package kr.maribel.backend.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import kr.maribel.backend.domain.DomainEnums.ServerOpenStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "maribel")
public class MaribelProperties {

    private boolean devLoginEnabled = true;
    private Upload upload = new Upload();
    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    /** 어드민으로 자동 승격할 마인크래프트 닉네임/UUID 목록 (로그인 시 SUPER_ADMIN 부여) */
    private List<String> adminMinecraft = new java.util.ArrayList<>();

    public List<String> getAdminMinecraft() {
        return adminMinecraft;
    }

    public void setAdminMinecraft(List<String> adminMinecraft) {
        this.adminMinecraft = adminMinecraft;
    }

    public Upload getUpload() {
        return upload;
    }

    public void setUpload(Upload upload) {
        this.upload = upload;
    }

    public boolean isDevLoginEnabled() {
        return devLoginEnabled;
    }

    public void setDevLoginEnabled(boolean devLoginEnabled) {
        this.devLoginEnabled = devLoginEnabled;
    }
    private ServerStatus serverStatus = new ServerStatus();
    private Webpanel webpanel = new Webpanel();
    private Microsoft microsoft = new Microsoft();
    private Stella stella = new Stella();
    private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();
    private LoginRateLimit loginRateLimit = new LoginRateLimit();
    private Cash cash = new Cash();

    public LoginRateLimit getLoginRateLimit() {
        return loginRateLimit;
    }

    public void setLoginRateLimit(LoginRateLimit loginRateLimit) {
        this.loginRateLimit = loginRateLimit;
    }

    public Cash getCash() {
        return cash;
    }

    public void setCash(Cash cash) {
        this.cash = cash;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public ServerStatus getServerStatus() {
        return serverStatus;
    }

    public void setServerStatus(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    public Webpanel getWebpanel() {
        return webpanel;
    }

    public void setWebpanel(Webpanel webpanel) {
        this.webpanel = webpanel;
    }

    public Microsoft getMicrosoft() {
        return microsoft;
    }

    public void setMicrosoft(Microsoft microsoft) {
        this.microsoft = microsoft;
    }

    public Stella getStella() {
        return stella;
    }

    public void setStella(Stella stella) {
        this.stella = stella;
    }

    public BootstrapAdmin getBootstrapAdmin() {
        return bootstrapAdmin;
    }

    public void setBootstrapAdmin(BootstrapAdmin bootstrapAdmin) {
        this.bootstrapAdmin = bootstrapAdmin;
    }

    public static class Jwt {
        private String issuer = "maribel-backend";
        private String secret = "dev-only-change-this-secret-before-production-please";
        private Duration accessTokenTtl = Duration.ofMinutes(15);
        private Duration memberRefreshTokenTtl = Duration.ofDays(14);
        private Duration adminRefreshTokenTtl = Duration.ofHours(12);
        private String refreshCookieName = "maribel_refresh";
        private String refreshCookiePath = "/api/auth";
        private boolean refreshCookieSecure;
        private String refreshCookieSameSite = "Lax";

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Duration getAccessTokenTtl() {
            return accessTokenTtl;
        }

        public void setAccessTokenTtl(Duration accessTokenTtl) {
            this.accessTokenTtl = accessTokenTtl;
        }

        public Duration getMemberRefreshTokenTtl() {
            return memberRefreshTokenTtl;
        }

        public void setMemberRefreshTokenTtl(Duration memberRefreshTokenTtl) {
            this.memberRefreshTokenTtl = memberRefreshTokenTtl;
        }

        public Duration getAdminRefreshTokenTtl() {
            return adminRefreshTokenTtl;
        }

        public void setAdminRefreshTokenTtl(Duration adminRefreshTokenTtl) {
            this.adminRefreshTokenTtl = adminRefreshTokenTtl;
        }

        public String getRefreshCookieName() {
            return refreshCookieName;
        }

        public void setRefreshCookieName(String refreshCookieName) {
            this.refreshCookieName = refreshCookieName;
        }

        public String getRefreshCookiePath() {
            return refreshCookiePath;
        }

        public void setRefreshCookiePath(String refreshCookiePath) {
            this.refreshCookiePath = refreshCookiePath;
        }

        public boolean isRefreshCookieSecure() {
            return refreshCookieSecure;
        }

        public void setRefreshCookieSecure(boolean refreshCookieSecure) {
            this.refreshCookieSecure = refreshCookieSecure;
        }

        public String getRefreshCookieSameSite() {
            return refreshCookieSameSite;
        }

        public void setRefreshCookieSameSite(String refreshCookieSameSite) {
            this.refreshCookieSameSite = refreshCookieSameSite;
        }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:3000,http://localhost:5173";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> allowedOriginList() {
            return Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .toList();
        }
    }

    public static class ServerStatus {
        private ServerOpenStatus status = ServerOpenStatus.OPEN;
        private String message = "마리벨 서버가 열려 있습니다.";
        private int onlinePlayers;

        public ServerOpenStatus getStatus() {
            return status;
        }

        public void setStatus(ServerOpenStatus status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getOnlinePlayers() {
            return onlinePlayers;
        }

        public void setOnlinePlayers(int onlinePlayers) {
            this.onlinePlayers = onlinePlayers;
        }
    }

    public static class Webpanel {
        private String apiKey = "dev-webpanel-key-change-me";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class Microsoft {
        private String clientId = "";
        private String clientSecret = "";
        private String tenant = "consumers";
        private String redirectUri = "http://localhost:8080/api/auth/microsoft/callback";
        private String scopes = "XboxLive.signin offline_access openid email";
        private String successRedirectUri = "http://localhost:3000/auth/callback";
        private String failureRedirectUri = "http://localhost:3000/login";

        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank()
                    && clientSecret != null && !clientSecret.isBlank();
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getTenant() {
            return tenant;
        }

        public void setTenant(String tenant) {
            this.tenant = tenant;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getScopes() {
            return scopes;
        }

        public void setScopes(String scopes) {
            this.scopes = scopes;
        }

        public String getSuccessRedirectUri() {
            return successRedirectUri;
        }

        public void setSuccessRedirectUri(String successRedirectUri) {
            this.successRedirectUri = successRedirectUri;
        }

        public String getFailureRedirectUri() {
            return failureRedirectUri;
        }

        public void setFailureRedirectUri(String failureRedirectUri) {
            this.failureRedirectUri = failureRedirectUri;
        }
    }

    public static class Cash {
        // 결제 금액(원) → 지급 캐시 환율. 기본 1원 = 1캐시. 서버가 지급량을 산정하는 기준. (점검 H1)
        private long krwPerCash = 1L;

        public long getKrwPerCash() {
            return krwPerCash;
        }

        public void setKrwPerCash(long krwPerCash) {
            this.krwPerCash = krwPerCash;
        }
    }

    public static class Stella {
        private String merchantId = "";
        private String webhookSecret = "dev-stella-secret-change-me";
        private String paymentBaseUrl = "https://stella.example.invalid/pay";
        // 로컬 개발에서 서명 헤더 없이 웹훅을 테스트하기 위한 스위치. 운영(prod)에서는 반드시 false.
        private boolean allowUnsignedWebhook = true;

        public boolean isAllowUnsignedWebhook() {
            return allowUnsignedWebhook;
        }

        public void setAllowUnsignedWebhook(boolean allowUnsignedWebhook) {
            this.allowUnsignedWebhook = allowUnsignedWebhook;
        }

        public String getMerchantId() {
            return merchantId;
        }

        public void setMerchantId(String merchantId) {
            this.merchantId = merchantId;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public String getPaymentBaseUrl() {
            return paymentBaseUrl;
        }

        public void setPaymentBaseUrl(String paymentBaseUrl) {
            this.paymentBaseUrl = paymentBaseUrl;
        }
    }

    public static class LoginRateLimit {
        // 관리자 로그인 실패가 window 안에서 maxFailures 를 넘으면 해당 IP 를 차단한다.
        private int maxFailures = 10;
        private Duration window = Duration.ofMinutes(1);

        public int getMaxFailures() {
            return maxFailures;
        }

        public void setMaxFailures(int maxFailures) {
            this.maxFailures = maxFailures;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }

    public static class Upload {
        private String dir = "./uploads";
        private String publicBaseUrl = "http://localhost:8080";

        public String getDir() {
            return dir;
        }

        public void setDir(String dir) {
            this.dir = dir;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }
    }

    public static class BootstrapAdmin {
        private String username = "admin";
        private String password = "change-me-now";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
