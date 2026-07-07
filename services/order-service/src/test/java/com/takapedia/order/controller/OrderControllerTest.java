package com.takapedia.order.controller;

import com.takapedia.order.entity.Order;
import com.takapedia.order.entity.OrderStatus;
import com.takapedia.order.entity.PaymentMethod;
import com.takapedia.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    @WithMockUser
    void createOrder_returns202WithOrderIdAndStatus() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Order saved = new Order();
        saved.setId(orderId);
        saved.setProductId(productId);
        saved.setQuantity(3);
        saved.setStatus(OrderStatus.WAITING_PAYMENT);
        saved.setPaymentMethod(PaymentMethod.BCA_VA);
        saved.setAmount(BigDecimal.valueOf(30_000));

        when(orderService.createOrder(any(UUID.class), anyInt(), any(PaymentMethod.class), anyString()))
                .thenReturn(saved);

        String body = """
                {
                  "productId": "%s",
                  "quantity": 3,
                  "paymentMethod": "BCA_VA",
                  "shippingAddress": "Jl. Mawar No. 1"
                }
                """.formatted(productId);

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isAccepted())                    // 202
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("WAITING_PAYMENT"))
                .andExpect(jsonPath("$.amount").value(30000));
    }

    @Test
    @WithMockUser
    void createOrder_rejectsInvalidQuantity() throws Exception {
        UUID productId = UUID.randomUUID();

        String body = """
                {
                  "productId": "%s",
                  "quantity": 0,
                  "paymentMethod": "BCA_VA"
                }
                """.formatted(productId);

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());                 // 400
    }
}