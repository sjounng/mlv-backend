package kr.maribel.backend.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import kr.maribel.backend.config.MaribelProperties;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenCookieService {

    private final MaribelProperties properties;

    public RefreshTokenCookieService(MaribelProperties properties) {
        this.properties = properties;
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> properties.getJwt().getRefreshCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    public void write(HttpServletResponse response, String refreshToken, long maxAgeSeconds) {
        Cookie cookie = baseCookie(refreshToken);
        cookie.setMaxAge((int) Math.min(maxAgeSeconds, Integer.MAX_VALUE));
        response.addCookie(cookie);
    }

    public void clear(HttpServletResponse response) {
        Cookie cookie = baseCookie("");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private Cookie baseCookie(String value) {
        MaribelProperties.Jwt jwt = properties.getJwt();
        Cookie cookie = new Cookie(jwt.getRefreshCookieName(), value);
        cookie.setHttpOnly(true);
        cookie.setSecure(jwt.isRefreshCookieSecure());
        cookie.setPath(jwt.getRefreshCookiePath());
        cookie.setAttribute("SameSite", jwt.getRefreshCookieSameSite());
        return cookie;
    }
}
