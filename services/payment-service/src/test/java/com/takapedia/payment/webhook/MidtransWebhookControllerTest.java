package com.takapedia.payment.webhook;

import com.takapedia.payment.event.PaymentCompletedEvent;
import com.takapedia.payment.event.PaymentEventPublisher;
import com.takapedia.payment.event.PaymentOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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

    @MockitoBean
    private PaymentEventPublisher paymentEventPublisher;

    @Test
    void handle_returns200AndPublishes_whenTransitioned() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(signatureVerifier.isValid(any(), any(), any())).thenReturn(true);
        when(webhookProcessingService.process(any(), any()))
                .thenReturn(WebhookProcessingResult.transitioned(
                        PaymentOutcome.SUCCESS, "VA-BCA-12345678", null));

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

        verify(paymentEventPublisher).publish(any(PaymentCompletedEvent.class));
    }

    @Test
    void handle_returns401AndDoesNotPublish_whenSignatureInvalid() throws Exception {
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

        verify(paymentEventPublisher, never()).publish(any());
    }

    @Test
    void handle_returns200AndDoesNotPublish_whenSkipped() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(signatureVerifier.isValid(any(), any(), any())).thenReturn(true);
        when(webhookProcessingService.process(any(), any()))
                .thenReturn(WebhookProcessingResult.skipped());

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

        verify(paymentEventPublisher, never()).publish(any());
    }

    @Test
    void handle_returns200AndDoesNotPublish_whenOrderNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(signatureVerifier.isValid(any(), any(), any())).thenReturn(true);
        when(webhookProcessingService.process(any(), any()))
                .thenReturn(WebhookProcessingResult.orderNotFound());

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

        verify(paymentEventPublisher, never()).publish(any());
    }
}