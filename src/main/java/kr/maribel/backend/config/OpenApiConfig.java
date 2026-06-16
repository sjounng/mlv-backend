package kr.maribel.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Maribel Backend API",
                version = "0.1.0",
                description = "마리벨 공식 웹사이트 백엔드 API 문서입니다. 사용자 인증, 웹상점, 결제, 이벤트 보상, 인게임 우편 큐, 관리자 기능을 포함합니다.",
                contact = @Contact(name = "Maribel Backend", email = "admin@maribel.local"),
                license = @License(name = "Internal")
        ),
        tags = {
                @Tag(name = "Auth", description = "회원 인증과 Microsoft OAuth 진입점"),
                @Tag(name = "Admin Auth", description = "관리자 인증"),
                @Tag(name = "Public", description = "공개 서버 상태와 팝업"),
                @Tag(name = "Legal", description = "약관, 개인정보처리방침, 환불 정책"),
                @Tag(name = "Shop", description = "카테고리, 상품, 캐시 충전, 구매"),
                @Tag(name = "Me", description = "마이페이지와 사용자 내역"),
                @Tag(name = "Events", description = "이벤트 보상과 리딤코드"),
                @Tag(name = "Contact", description = "문의 작성과 내역"),
                @Tag(name = "Webpanel", description = "마크 서버 웹패널 우편 큐 연동"),
                @Tag(name = "Admin", description = "운영자 관리 API")
        }
)
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT access token"
)
@SecurityScheme(
        name = OpenApiConfig.WEBPANEL_KEY,
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-Maribel-Webpanel-Key",
        description = "웹패널 폴링/ACK API 키"
)
@SecurityScheme(
        name = OpenApiConfig.STELLA_SIGNATURE,
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-Stella-Signature",
        description = "Stella IT 웹훅 서명"
)
@SecurityScheme(
        name = OpenApiConfig.REFRESH_COOKIE,
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = "maribel_refresh",
        description = "HttpOnly refresh token cookie"
)
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";
    public static final String WEBPANEL_KEY = "webpanelApiKey";
    public static final String STELLA_SIGNATURE = "stellaSignature";
    public static final String REFRESH_COOKIE = "refreshCookie";

    @Bean
    OpenAPI maribelOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development"),
                        new Server().url("https://api.maribel.local").description("Production placeholder")
                ));
    }
}
