package com.takapedia.payment.webhook;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MidtransWebhookController.class)
class MidtransWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebhookSignatureVerifier signatureVerifier;

    @MockitoBean
    private WebhookProcessingService webhookProcessingService;

    @Test
    void handle_returns200_whenSignatureValidAndTransitioned() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(signatureVerifier.isValid(any(), any(), any())).thenReturn(true);
        when(webhookProcessingService.process(any(), any()))
                .thenReturn(WebhookProcessingService.ProcessResult.TRANSITIONED);

        String body = """
                {
                  "orderId": "%s",
                  "transactionStatus": "settlement",
                  "signatureKey": "valid-sig"
                }
                """.formatted(orderId);

        mockMvc.perform(post("/webhooks/midtrans")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void handle_returns401_whenSignatureInvalid() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(signatureVerifier.isValid(any(), any(), any())).thenReturn(false);

        String body = """
                {
                  "orderId": "%s",
                  "transactionStatus": "settlement",
                  "signatureKey": "tampered"
                }
                """.formatted(orderId);

        mockMvc.perform(post("/webhooks/midtrans")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void handle_returns200_whenSignatureValidButOrderNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(signatureVerifier.isValid(any(), any(), any())).thenReturn(true);
        when(webhookProcessingService.process(any(), any()))
                .thenReturn(WebhookProcessingService.ProcessResult.ORDER_NOT_FOUND);

        String body = """
                {
                  "orderId": "%s",
                  "transactionStatus": "settlement",
                  "signatureKey": "valid-sig"
                }
                """.formatted(orderId);

        mockMvc.perform(post("/webhooks/midtrans")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
    }
}