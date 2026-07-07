package com.takapedia.payment.controller;

import com.takapedia.payment.config.SecurityConfig;
import com.takapedia.payment.dto.PaymentResponse;
import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.entity.PaymentMethod;
import com.takapedia.payment.entity.PaymentStatus;
import com.takapedia.payment.service.PaymentQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentQueryService paymentQueryService;

    private PaymentResponse sampleResponse(UUID orderId) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(BigDecimal.valueOf(30_000));
        payment.setPaymentMethod(PaymentMethod.BCA_VA);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setExternalReference("VA-BCA-12345678");
        return PaymentResponse.from(payment);
    }

    @Test
    @WithMockUser
    void getByOrderId_returns200WithPayment_whenExists() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(paymentQueryService.getByOrderId(orderId)).thenReturn(sampleResponse(orderId));

        mockMvc.perform(get("/api/v1/payments/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentMethod").value("BCA_VA"))
                .andExpect(jsonPath("$.amount").value(30000))
                .andExpect(jsonPath("$.externalReference").value("VA-BCA-12345678"));
    }

    @Test
    @WithMockUser
    void getByOrderId_returns404_whenNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(paymentQueryService.getByOrderId(any(UUID.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/payments/{orderId}", orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByOrderId_returns401_whenNoJwt() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/payments/{orderId}", orderId))
                .andExpect(status().isUnauthorized());
    }
}
