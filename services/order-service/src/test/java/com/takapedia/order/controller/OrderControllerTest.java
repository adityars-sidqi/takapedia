package com.takapedia.order.controller;

import com.takapedia.order.entity.Order;
import com.takapedia.order.entity.OrderStatus;
import com.takapedia.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
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
        saved.setStatus(OrderStatus.PENDING);

        when(orderService.createOrder(any(UUID.class), anyInt())).thenReturn(saved);

        String body = """
                {
                  "productId": "%s",
                  "quantity": 3
                }
                """.formatted(productId);

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isAccepted())                    // 202
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser
    void createOrder_rejectsInvalidQuantity() throws Exception {
        UUID productId = UUID.randomUUID();

        String body = """
                {
                  "productId": "%s",
                  "quantity": 0
                }
                """.formatted(productId);

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());                 // 400
    }
}