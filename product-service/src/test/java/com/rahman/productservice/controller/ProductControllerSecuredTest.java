package com.rahman.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rahman.productservice.ProductServiceApplication;
import com.rahman.productservice.dto.product.CreateProductRequest;
import com.rahman.productservice.entity.Category;
import com.rahman.productservice.entity.Product;
import com.rahman.productservice.entity.ProductTag;
import com.rahman.productservice.entity.Tag;
import com.rahman.productservice.repository.CategoryRepository;
import com.rahman.productservice.repository.ProductRepository;
import com.rahman.productservice.repository.TagRepository;
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
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

        @Autowired
        private CategoryRepository categoryRepository;

        @Autowired
        private TagRepository tagRepository;

        private Category category1;

        private Tag tag1;

        @BeforeEach
        void setUp() {
                productRepository.deleteAll();
                categoryRepository.deleteAll();
                tagRepository.deleteAll();

                category1 = new Category();
                category1.setName("Shampoo Korea");
                category1.setDescription("Description shampoo");

                categoryRepository.save(category1);

                tag1 = new Tag();
                tag1.setName("Organic");

                tagRepository.save(tag1);
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

        @Test
        @WithMockUser(username = "user", roles = "USER")
        void testDeleteProduct_WhenRoleIsUser_ReturnsBadRequest() throws Exception {
                //Prepare data
                Product productData = new Product();
                productData.setName("Shampoo Korea Bagus");
                productData.setDescription("Ini shampo original Korea lohhh");
                productData.setPrice(new BigDecimal(150000));
                productData.setStock(100);
                productData.setCreatedAt(Instant.now());
                productData.setUpdatedAt(Instant.now());

                Category attachedCategory = categoryRepository.findById(category1.getId())
                        .orElseThrow(() -> new RuntimeException("Category not found"));

                productData.setCategory(attachedCategory);

                Product productSaved = productRepository.save(productData);

                Tag attachedTag = tagRepository.findById(tag1.getId())
                        .orElseThrow(() -> new RuntimeException("Tag not found"));

                ProductTag productTagData = new ProductTag(productSaved, attachedTag);
                productSaved.getProductTags().add(productTagData);

                productRepository.save(productSaved);

                UUID id = productSaved.getId();


                mockMvc.perform(delete("/" + id)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isForbidden());
        }

        @Test
        void testDeleteProduct_WhenNotAuthenticated_ReturnsUnauthorized() throws Exception {
                //Prepare data
                //Prepare data
                Product productData = new Product();
                productData.setName("Shampoo Korea Bagus");
                productData.setDescription("Ini shampo original Korea lohhh");
                productData.setPrice(new BigDecimal(150000));
                productData.setStock(100);
                productData.setCreatedAt(Instant.now());
                productData.setUpdatedAt(Instant.now());

                Category attachedCategory = categoryRepository.findById(category1.getId())
                        .orElseThrow(() -> new RuntimeException("Category not found"));

                productData.setCategory(attachedCategory);

                Product productSaved = productRepository.save(productData);

                Tag attachedTag = tagRepository.findById(tag1.getId())
                        .orElseThrow(() -> new RuntimeException("Tag not found"));

                ProductTag productTagData = new ProductTag(productSaved, attachedTag);
                productSaved.getProductTags().add(productTagData);

                productRepository.save(productSaved);

                UUID id = productSaved.getId();


                mockMvc.perform(delete("/" + id)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isUnauthorized());
        }
}
