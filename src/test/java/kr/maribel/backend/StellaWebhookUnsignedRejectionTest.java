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

/**
 * 운영 설정(allow-unsigned-webhook=false)에서는 서명 헤더 없는 웹훅이 반드시 거부되어야 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "maribel.stella.allow-unsigned-webhook=false")
class StellaWebhookUnsignedRejectionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unsignedWebhookIsRejectedWhenNotAllowed() throws Exception {
        mockMvc.perform(post("/api/payments/stella/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantOrderId":"cash_does_not_matter",
                                  "stellaPaymentId":"pay_x",
                                  "status":"PAID",
                                  "paidAmountKrw":1000
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_STELLA_SIGNATURE"));
    }
}
