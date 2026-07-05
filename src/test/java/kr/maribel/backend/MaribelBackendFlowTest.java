package kr.maribel.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaribelBackendFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsAndScalarUiArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Maribel Backend API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.webpanelApiKey.name").value("X-Maribel-Webpanel-Key"))
                .andExpect(jsonPath("$.components.securitySchemes.stellaSignature.name").value("X-Stella-Signature"))
                .andExpect(jsonPath("$.components.securitySchemes.refreshCookie.in").value("cookie"));

        mockMvc.perform(get("/scalar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"url\":\"/v3/api-docs\"")))
                .andExpect(content().string(containsString("\"searchHotKey\":\"k\"")));

        mockMvc.perform(get("/scalar/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/scalar"));
    }

    @Test
    void refreshTokenIsStoredInHttpOnlyCookieAndRotates() throws Exception {
        MvcResult loginResult = devLogin()
                .andExpect(status().isOk())
                .andExpect(cookie().exists("maribel_refresh"))
                .andReturn();

        Cookie firstCookie = loginResult.getResponse().getCookie("maribel_refresh");
        String firstRefreshToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.refreshToken");
        assertThat(firstCookie).isNotNull();
        assertThat(firstCookie.getValue()).isEqualTo(firstRefreshToken);
        assertThat(firstCookie.isHttpOnly()).isTrue();
        assertThat(firstCookie.getPath()).isEqualTo("/api/auth");
        assertThat(firstCookie.getMaxAge()).isEqualTo((int) Duration.ofDays(14).toSeconds());

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh").cookie(firstCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("maribel_refresh"))
                .andReturn();
        Cookie secondCookie = refreshResult.getResponse().getCookie("maribel_refresh");
        String secondRefreshToken = JsonPath.read(refreshResult.getResponse().getContentAsString(), "$.refreshToken");
        assertThat(secondCookie).isNotNull();
        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);
        assertThat(secondCookie.getValue()).isEqualTo(secondRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(firstRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));

        mockMvc.perform(post("/api/auth/refresh").cookie(secondCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutClearsCookieAndRevokesRefreshToken() throws Exception {
        MvcResult loginResult = devLogin()
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshCookie = loginResult.getResponse().getCookie("maribel_refresh");

        mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("maribel_refresh", 0));

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void userCanChargePurchaseAndReceiveMailThroughWebpanelFlow() throws Exception {
        String accessToken = login();

        String chargeJson = mockMvc.perform(post("/api/shop/cash/charges")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cashAmount":1000,"paymentAmountKrw":1000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String merchantOrderId = JsonPath.read(chargeJson, "$.merchantOrderId");

        mockMvc.perform(post("/api/payments/stella/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantOrderId":"%s",
                                  "stellaPaymentId":"pay_test_1",
                                  "status":"PAID",
                                  "paidAmountKrw":1000,
                                  "receiptUrl":"https://receipt.example/test"
                                }
                                """.formatted(merchantOrderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(get("/api/me/cash").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000));

        String productsJson = mockMvc.perform(get("/api/shop/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer productId = JsonPath.read(productsJson, "$.content[0].id");

        mockMvc.perform(post("/api/shop/purchases")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":%d,"quantity":1}
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAIL_PENDING"))
                .andExpect(jsonPath("$.outboundMail.status").value("PENDING"));

        String pendingMailJson = mockMvc.perform(get("/api/webpanel/mails/pending")
                        .header("X-Maribel-Webpanel-Key", "test-webpanel-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer mailId = JsonPath.read(pendingMailJson, "$[0].id");

        mockMvc.perform(post("/api/webpanel/mails/{id}/ack", mailId)
                        .header("X-Maribel-Webpanel-Key", "test-webpanel-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SENT","retryable":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));

        mockMvc.perform(get("/api/me/mails").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }

    @Test
    void adminCanSearchMembersWithPaginationAndApplySanctions() throws Exception {
        login(); // 회원 MaribelTester 생성
        String adminToken = adminLogin();

        // 키워드 검색 + 페이지네이션 응답 구조 확인
        String membersJson = mockMvc.perform(get("/api/admin/members")
                        .param("keyword", "Maribel")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content[0].minecraftUsername").value("MaribelTester"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer memberId = JsonPath.read(membersJson, "$.content[0].id");

        // 제재(일시정지)
        mockMvc.perform(patch("/api/admin/members/{id}/suspend", memberId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        // 상태 필터로 조회되는지
        mockMvc.perform(get("/api/admin/members")
                        .param("status", "SUSPENDED")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(memberId));

        // 제재 해제
        mockMvc.perform(patch("/api/admin/members/{id}/activate", memberId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private String adminLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"adminpass"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String login() throws Exception {
        MvcResult result = devLogin()
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private org.springframework.test.web.servlet.ResultActions devLogin() throws Exception {
        return mockMvc.perform(post("/api/auth/dev-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "microsoftSub":"ms-test-user",
                          "minecraftUuid":"00000000-0000-0000-0000-000000000001",
                          "minecraftUsername":"MaribelTester",
                          "email":"tester@example.com",
                          "marketingAgreed":false
                        }
                        """));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
