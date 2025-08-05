package com.rahman.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rahman.productservice.ProductServiceApplication;
import com.rahman.productservice.dto.product.CreateProductRequest;
import com.rahman.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = ProductServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.fail-fast=false",
                "spring.cloud.discovery.enabled=false",
                "eureka.client.enabled=false"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("secured-test")
class ProductControllerSecuredTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private ProductRepository productRepository;

        @BeforeEach
        void setUp() {
                productRepository.deleteAll();
        }

        @Test
        @WithMockUser(username = "user", roles = "USER")
        void testSaveProduct_WhenRoleIsUser_ReturnsBadRequest() throws Exception {
                CreateProductRequest request = new CreateProductRequest("Shampoo Korea Bagus",
                        "Ini shampo original Korea lohhh",
                        new BigDecimal(150000),
                        100,
                        UUID.randomUUID(), Set.of(UUID.randomUUID(), UUID.randomUUID()));

                mockMvc.perform(post("/")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isForbidden());
        }

        @Test
        void testSaveProduct_WhenNotAuthenticated_ReturnsUnauthorized() throws Exception {
                CreateProductRequest request = new CreateProductRequest("Shampoo Korea Bagus",
                        "Ini shampo original Korea lohhh",
                        new BigDecimal(150000),
                        100,
                        UUID.randomUUID(), Set.of(UUID.randomUUID(), UUID.randomUUID()));

                mockMvc.perform(post("/")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
        }
}
