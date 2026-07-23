package kr.maribel.backend.config;

import java.util.List;
import kr.maribel.backend.security.JwtAuthenticationFilter;
import kr.maribel.backend.security.WebpanelApiKeyFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationFilter jwtAuthenticationFilter,
                                            WebpanelApiKeyFilter webpanelApiKeyFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info", "/h2-console/**", "/v3/api-docs/**", "/scalar", "/scalar/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/api/auth/microsoft/**", "/api/auth/dev-login", "/api/auth/refresh", "/api/auth/logout", "/api/admin/auth/login").permitAll()
                        // 출석 보드는 인증 필요 — /api/events/* permitAll 보다 먼저 매칭시킨다.
                        .requestMatchers(HttpMethod.GET, "/api/events/attendance").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/public/**", "/api/legal/**", "/api/shop/categories", "/api/shop/products", "/api/shop/products/**", "/api/shop/cash-products", "/api/shop/cash-products/**", "/api/shop/cash-product-description", "/api/events", "/api/events/featured", "/api/events/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/stella/webhook").permitAll()
                        .requestMatchers("/api/webpanel/**").hasRole("WEBPANEL")
                        // 권한(역할) 변경은 최고 관리자만 (일반 /api/admin/** 규칙보다 먼저 매칭)
                        .requestMatchers(HttpMethod.PATCH, "/api/admin/members/*/role").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("OPERATOR", "SUPER_ADMIN")
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(webpanelApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(MaribelProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.getCors().allowedOriginList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Maribel-Webpanel-Key", "X-Stella-Signature"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }
}
