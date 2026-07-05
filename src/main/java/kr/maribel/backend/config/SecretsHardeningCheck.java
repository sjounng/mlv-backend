package kr.maribel.backend.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 운영 프로파일(prod)로 기동할 때 개발용 기본 시크릿/비밀번호가 그대로 남아 있으면
 * 애플리케이션 부팅을 중단시킨다. 로컬/테스트 환경에는 영향이 없다.
 */
@Component
@Order(0)
public class SecretsHardeningCheck implements ApplicationRunner {

    private static final String DEFAULT_JWT_SECRET = "dev-only-change-this-secret-before-production-please";
    private static final String DEFAULT_WEBPANEL_KEY = "dev-webpanel-key-change-me";
    private static final String DEFAULT_STELLA_SECRET = "dev-stella-secret-change-me";
    private static final String DEFAULT_ADMIN_PASSWORD = "change-me-now";

    private final MaribelProperties properties;
    private final Environment environment;

    public SecretsHardeningCheck(MaribelProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean prod = List.of(environment.getActiveProfiles()).contains("prod");
        if (!prod) {
            return;
        }

        List<String> violations = new ArrayList<>();
        if (DEFAULT_JWT_SECRET.equals(properties.getJwt().getSecret())) {
            violations.add("MARIBEL_JWT_SECRET");
        }
        if (DEFAULT_WEBPANEL_KEY.equals(properties.getWebpanel().getApiKey())) {
            violations.add("MARIBEL_WEBPANEL_API_KEY");
        }
        if (DEFAULT_STELLA_SECRET.equals(properties.getStella().getWebhookSecret())) {
            violations.add("STELLA_WEBHOOK_SECRET");
        }
        if (DEFAULT_ADMIN_PASSWORD.equals(properties.getBootstrapAdmin().getPassword())) {
            violations.add("MARIBEL_BOOTSTRAP_ADMIN_PASSWORD");
        }
        if (properties.isDevLoginEnabled()) {
            violations.add("MARIBEL_DEV_LOGIN_ENABLED(=false 필요)");
        }
        if (properties.getStella().isAllowUnsignedWebhook()) {
            violations.add("STELLA_ALLOW_UNSIGNED_WEBHOOK(=false 필요)");
        }

        if (!violations.isEmpty()) {
            throw new IllegalStateException(
                    "운영(prod) 환경에서 다음 보안 설정이 기본값이거나 안전하지 않습니다. 환경변수로 설정하세요: "
                            + String.join(", ", violations));
        }
    }
}
