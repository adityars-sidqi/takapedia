package com.rahman.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rahman.productservice.ProductServiceApplication;
import com.rahman.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
}
