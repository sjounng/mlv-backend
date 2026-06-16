package kr.maribel.backend.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import kr.maribel.backend.domain.DomainEnums.ServerOpenStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "maribel")
public class MaribelProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private ServerStatus serverStatus = new ServerStatus();
    private Webpanel webpanel = new Webpanel();
    private Microsoft microsoft = new Microsoft();
    private Stella stella = new Stella();
    private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();

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
        private String redirectUri = "http://localhost:8080/api/auth/microsoft/callback";
        private String scopes = "XboxLive.signin offline_access";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
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
    }

    public static class Stella {
        private String merchantId = "";
        private String webhookSecret = "dev-stella-secret-change-me";
        private String paymentBaseUrl = "https://stella.example.invalid/pay";

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
