package kr.maribel.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import kr.maribel.backend.config.MaribelProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 웹패널 전용 엔드포인트(/api/webpanel/**)를 보안 레이어에서 API 키로 검증한다.
 * 서비스 레이어의 키 검증과 함께 이중 방어를 구성하며, 키가 유효하면
 * ROLE_WEBPANEL 권한을 부여해 SecurityConfig 의 인가 규칙을 통과시킨다.
 */
@Component
public class WebpanelApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Maribel-Webpanel-Key";

    private final MaribelProperties properties;

    public WebpanelApiKeyFilter(MaribelProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/webpanel/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER);
        if (!StringUtils.hasText(apiKey) || !matches(apiKey, properties.getWebpanel().getApiKey())) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"INVALID_WEBPANEL_KEY\",\"message\":\"웹패널 API 키가 올바르지 않습니다.\"}");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "webpanel",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_WEBPANEL"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean matches(String provided, String expected) {
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
