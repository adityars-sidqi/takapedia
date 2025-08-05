package com.rahman.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rahman.productservice.dto.category.CategorySimpleResponse;
import com.rahman.productservice.dto.product.CreateProductRequest;
import com.rahman.productservice.dto.product.ProductResponse;
import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.entity.Category;
import com.rahman.productservice.entity.Product;
import com.rahman.productservice.entity.ProductTag;
import com.rahman.productservice.entity.Tag;
import com.rahman.productservice.mapper.ProductMapper;
import com.rahman.productservice.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.fail-fast=false"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ProductService productService;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private ProductMapper productMapper;

        @Autowired
        private MessageSource messageSource;

        @Test
        @WithMockUser(roles = "ADMIN")
        void testFindAll_ReturnsListOfProducts() throws Exception {
                // Arrange
                UUID id = UUID.randomUUID();
                ProductResponse productResponse1 = new ProductResponse(id,
                        "Shampoo Korea Bagus",
                        "Description shampoo korea bagus",
                        new BigDecimal(150000),
                        100,
                        new CategorySimpleResponse(UUID.randomUUID(), "Shampoo"),
                        List.of(new TagResponse(UUID.randomUUID(), "Bagus"),
                                new TagResponse(UUID.randomUUID(), "Korea")),
                        Instant.now(),
                        Instant.now());
                ProductResponse productResponse2 = new ProductResponse(id,
                        "Sabun Korea Bagus",
                        "Description Sabun korea bagus",
                        new BigDecimal(150000),
                        100,
                        new CategorySimpleResponse(UUID.randomUUID(), "Sabun"),
                        List.of(new TagResponse(UUID.randomUUID(), "Sabun"),
                                new TagResponse(UUID.randomUUID(), "Korea")),
                        Instant.now(),
                        Instant.now());

                when(productService.findAll()).thenReturn(List.of(productResponse1, productResponse2));

                // Act & Assert
                mockMvc.perform(get("/")
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success", is(true)))
                        .andExpect(jsonPath("$.message", is("success")))
                        .andExpect(jsonPath("$.data", hasSize(2)))
                        .andExpect(jsonPath("$.data[0].id", is(id.toString())))
                        .andExpect(jsonPath("$.data[0].name", is("Shampoo Korea Bagus")));

                verify(productService).findAll();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void testSave_ReturnsSuccess() throws Exception {
                CreateProductRequest request = new CreateProductRequest("Shampoo Korea Bagus",
                        "Ini shampo original Korea lohhh",
                        new BigDecimal(150000),
                        100,
                        UUID.randomUUID(), Set.of(UUID.randomUUID(), UUID.randomUUID()));

                Category categoryData = new Category();
                categoryData.setId(UUID.randomUUID());
                categoryData.setName("Shampoo");
                categoryData.setDescription("Shampoo");
                categoryData.setCreatedAt(Instant.now());
                categoryData.setUpdatedAt(Instant.now());

                Tag tagData = new Tag();
                tagData.setId(UUID.randomUUID());
                tagData.setName("Bagus");



                Product productData = new Product();
                productData.setId(UUID.randomUUID());
                productData.setName("Shampoo Korea Bagus");
                productData.setDescription("Ini shampo original Korea lohhh");
                productData.setPrice(new BigDecimal(150000));
                productData.setStock(100);
                productData.setCategory(categoryData);
                productData.setCreatedAt(Instant.now());
                productData.setUpdatedAt(Instant.now());

                ProductTag productTagData = new ProductTag(productData, tagData);

                productData.setProductTags(Set.of(productTagData));

                when(productService.save(any(CreateProductRequest.class))).thenReturn(productMapper.toResponse(productData));

                mockMvc.perform(post("/")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success", is(true)))
                        .andExpect(jsonPath("$.message", is("success")))
                        .andExpect(jsonPath("$.data.name", is("Shampoo Korea Bagus")));
        }
}
