package kr.maribel.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "maribel.login-rate-limit.max-failures=3")
class AdminLoginRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void repeatedLoginFailuresAreBlockedWith429() throws Exception {
        for (int i = 0; i < 3; i++) {
            attemptLogin("wrong-password").andExpect(status().isUnauthorized());
        }

        // 한도를 넘으면 올바른 비밀번호로도 window 가 끝날 때까지 차단된다.
        attemptLogin("adminpass")
                .andExpect(status().is(429))
                .andExpect(jsonPath("$.code").value("TOO_MANY_LOGIN_ATTEMPTS"));
    }

    private org.springframework.test.web.servlet.ResultActions attemptLogin(String password) throws Exception {
        return mockMvc.perform(post("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"%s\"}".formatted(password)));
    }
}
