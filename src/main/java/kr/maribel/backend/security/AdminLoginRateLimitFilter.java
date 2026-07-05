package kr.maribel.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import kr.maribel.backend.config.MaribelProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 관리자 로그인 브루트포스 방어. IP 별로 실패 횟수를 세고,
 * window 안에서 maxFailures 를 넘으면 window 가 끝날 때까지 429 로 차단한다.
 */
@Component
@Order(-200) // Security 필터 체인보다 먼저 실행되어야 응답 상태로 실패를 집계할 수 있다.
public class AdminLoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/admin/auth/login";
    private static final int PRUNE_THRESHOLD = 10_000;

    private final MaribelProperties properties;
    private final ConcurrentHashMap<String, FailureWindow> failuresByIp = new ConcurrentHashMap<>();

    public AdminLoginRateLimitFilter(MaribelProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod()) && LOGIN_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        long now = System.currentTimeMillis();
        long windowMillis = properties.getLoginRateLimit().getWindow().toMillis();
        int maxFailures = properties.getLoginRateLimit().getMaxFailures();

        FailureWindow window = failuresByIp.get(ip);
        if (window != null && window.isBlocked(now, windowMillis, maxFailures)) {
            writeTooManyRequests(request, response);
            return;
        }

        filterChain.doFilter(request, response);

        if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            pruneIfNeeded(now, windowMillis);
            failuresByIp.computeIfAbsent(ip, key -> new FailureWindow()).recordFailure(now, windowMillis);
        } else if (response.getStatus() < 400) {
            failuresByIp.remove(ip);
        }
    }

    private void pruneIfNeeded(long now, long windowMillis) {
        if (failuresByIp.size() >= PRUNE_THRESHOLD) {
            failuresByIp.entrySet().removeIf(entry -> entry.getValue().isExpired(now, windowMillis));
        }
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"timestamp":"%s","status":429,"code":"TOO_MANY_LOGIN_ATTEMPTS","message":"로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.","path":"%s"}
                """.formatted(Instant.now(), request.getRequestURI()));
    }

    private static final class FailureWindow {
        private long windowStart;
        private int count;

        synchronized void recordFailure(long now, long windowMillis) {
            if (now - windowStart > windowMillis) {
                windowStart = now;
                count = 0;
            }
            count++;
        }

        synchronized boolean isBlocked(long now, long windowMillis, int maxFailures) {
            return now - windowStart <= windowMillis && count >= maxFailures;
        }

        synchronized boolean isExpired(long now, long windowMillis) {
            return now - windowStart > windowMillis;
        }
    }
}
