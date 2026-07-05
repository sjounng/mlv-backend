package kr.maribel.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 돈이 오가는 핵심 경로(웹훅 서명/금액 대사/멱등성, 구매 재고/캐시, 환불) 회귀 테스트.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopMoneyPathTest {

    private static final String TEST_STELLA_SECRET = "dev-stella-secret-change-me";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void webhookVerifiesSignatureWhenHeaderPresent() throws Exception {
        String accessToken = devLogin("sig-user");
        String merchantOrderId = createCharge(accessToken, 300);

        String body = webhookBody(merchantOrderId, "pay_sig_1", "PAID", 300);

        mockMvc.perform(post("/api/payments/stella/webhook")
                        .header("X-Stella-Signature", Base64.getEncoder().encodeToString("wrong".getBytes(StandardCharsets.UTF_8)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_STELLA_SIGNATURE"));

        mockMvc.perform(post("/api/payments/stella/webhook")
                        .header("X-Stella-Signature", sign(merchantOrderId, "pay_sig_1", "PAID", 300))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(get("/api/me/cash").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(300));
    }

    @Test
    void webhookRejectsAmountMismatch() throws Exception {
        String accessToken = devLogin("mismatch-user");
        String merchantOrderId = createCharge(accessToken, 700);

        mockMvc.perform(post("/api/payments/stella/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody(merchantOrderId, "pay_mismatch_1", "PAID", 100)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_AMOUNT_MISMATCH"));

        mockMvc.perform(get("/api/me/cash").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void webhookIsIdempotentForDuplicatePaidNotifications() throws Exception {
        String accessToken = devLogin("idem-user");
        String merchantOrderId = createCharge(accessToken, 500);

        payViaWebhook(merchantOrderId, "pay_idem_1", 500);
        payViaWebhook(merchantOrderId, "pay_idem_1", 500);

        mockMvc.perform(get("/api/me/cash").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500));
    }

    @Test
    void purchaseEnforcesStockAndDecrementsIt() throws Exception {
        String adminToken = adminLogin();
        Integer productId = createProduct(adminToken, "STOCK_TEST", 100, 1);

        String accessToken = devLogin("stock-user");
        String merchantOrderId = createCharge(accessToken, 1000);
        payViaWebhook(merchantOrderId, "pay_stock_1", 1000);

        mockMvc.perform(post("/api/shop/purchases")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"quantity\":2}".formatted(productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        mockMvc.perform(post("/api/shop/purchases")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"quantity\":1}".formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(100));

        mockMvc.perform(get("/api/shop/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(0));

        mockMvc.perform(post("/api/shop/purchases")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"quantity\":1}".formatted(productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void purchaseRejectsWhenCashInsufficient() throws Exception {
        String adminToken = adminLogin();
        Integer productId = createProduct(adminToken, "CASH_TEST", 900, null);

        String accessToken = devLogin("poor-user");

        mockMvc.perform(post("/api/shop/purchases")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"quantity\":1}".formatted(productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_CASH"));
    }

    @Test
    void refundCompletionDeductsBalanceExactlyOnce() throws Exception {
        String accessToken = devLogin("refund-user");
        String merchantOrderId = createCharge(accessToken, 400);
        payViaWebhook(merchantOrderId, "pay_refund_1", 400);

        String chargesJson = mockMvc.perform(get("/api/me/charges")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer chargeId = JsonPath.read(chargesJson, "$[0].id");

        String refundJson = mockMvc.perform(post("/api/me/refunds")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cashChargeId\":%d,\"reason\":\"테스트 환불\"}".formatted(chargeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer refundId = JsonPath.read(refundJson, "$.id");

        String adminToken = adminLogin();
        mockMvc.perform(patch("/api/admin/refunds/{id}/process", refundId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\",\"operatorMemo\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/me/cash").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0));

        // 같은 충전 건을 다시 COMPLETED 처리하면 이중 차감 없이 거부되어야 한다.
        mockMvc.perform(patch("/api/admin/refunds/{id}/process", refundId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\",\"operatorMemo\":\"again\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REFUND_ALREADY_PROCESSED"));
    }

    private String createCharge(String accessToken, long amount) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/shop/cash/charges")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cashAmount\":%d,\"paymentAmountKrw\":%d}".formatted(amount, amount)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.merchantOrderId");
    }

    /** 테스트 프로파일은 allow-unsigned-webhook 기본값(true)이라 서명 없이 결제 완료 처리할 수 있다. */
    private void payViaWebhook(String merchantOrderId, String paymentId, long amount) throws Exception {
        mockMvc.perform(post("/api/payments/stella/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody(merchantOrderId, paymentId, "PAID", amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    private String webhookBody(String merchantOrderId, String paymentId, String status, long paidAmountKrw) {
        return """
                {
                  "merchantOrderId":"%s",
                  "stellaPaymentId":"%s",
                  "status":"%s",
                  "paidAmountKrw":%d,
                  "receiptUrl":"https://receipt.example/test"
                }
                """.formatted(merchantOrderId, paymentId, status, paidAmountKrw);
    }

    private String sign(String merchantOrderId, String paymentId, String status, long paidAmountKrw) throws Exception {
        String payload = merchantOrderId + ":" + paymentId + ":" + status + ":" + paidAmountKrw;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_STELLA_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private Integer createProduct(String adminToken, String codePrefix, long price, Integer stockQuantity) throws Exception {
        MvcResult categoryResult = mockMvc.perform(post("/api/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s_카테고리\",\"sortOrder\":99,\"active\":true}".formatted(codePrefix)))
                .andExpect(status().isOk())
                .andReturn();
        Integer categoryId = JsonPath.read(categoryResult.getResponse().getContentAsString(), "$.id");

        MvcResult templateResult = mockMvc.perform(post("/api/admin/mail-templates")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mailCode":"%s_MAIL","subject":"테스트 우편","content":"테스트","rewardsJson":"[]"}
                                """.formatted(codePrefix)))
                .andExpect(status().isOk())
                .andReturn();
        Integer templateId = JsonPath.read(templateResult.getResponse().getContentAsString(), "$.id");

        MvcResult productResult = mockMvc.perform(post("/api/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s 상품","description":"테스트 상품","price":%d,"categoryId":%d,
                                 "mailTemplateId":%d,"active":true,%s"recommended":false,"newBadge":false}
                                """.formatted(codePrefix, price, categoryId, templateId,
                                stockQuantity == null ? "" : "\"stockQuantity\":" + stockQuantity + ",")))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(productResult.getResponse().getContentAsString(), "$.id");
    }

    private String adminLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"adminpass\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    /** 테스트 간 잔액이 섞이지 않도록 suffix 별로 별도 회원을 만든다. */
    private String devLogin(String suffix) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "microsoftSub":"ms-%s",
                                  "minecraftUuid":"00000000-0000-0000-0000-%012d",
                                  "minecraftUsername":"Tester_%s",
                                  "email":"%s@example.com",
                                  "marketingAgreed":false
                                }
                                """.formatted(suffix, Math.abs(suffix.hashCode()) % 1000000000L + 100, suffix, suffix)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
