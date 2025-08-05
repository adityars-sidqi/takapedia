package com.rahman.productservice.controller;

import com.rahman.commonlib.ApiResponse;
import com.rahman.productservice.ProductServiceApplication;
import com.rahman.productservice.dto.category.CategorySimpleResponse;
import com.rahman.productservice.dto.product.CreateProductRequest;
import com.rahman.productservice.dto.product.ProductResponse;
import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.entity.*;
import com.rahman.productservice.repository.CategoryRepository;
import com.rahman.productservice.repository.ProductRepository;
import com.rahman.productservice.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

        @LocalServerPort
        private int port;

        @Autowired
        private TestRestTemplate restTemplate;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private CategoryRepository categoryRepository;

        @Autowired
        private TagRepository tagRepository;

        private String baseUrl;

        private Category category1;
        private Category category2;

        private Tag tag1;
        private Tag tag2;
        private Tag tag3;
        private Tag tag4;

        @BeforeEach
        void setUp() {
                baseUrl = "http://localhost:" + port;

                category1 = new Category();
                category1.setName("Shampoo");
                category1.setDescription("Description shampoo");

                category2 = new Category();
                category2.setName("Sabun");
                category2.setDescription("Description sabun");

                categoryRepository.saveAll(List.of(category1, category2));

                tag1 = new Tag();
                tag1.setName("Korea");

                tag2 = new Tag();
                tag2.setName("Bagus");

                tag3 = new Tag();
                tag3.setName("Sabun");

                tag4 = new Tag();
                tag4.setName("Shampoo");

                tagRepository.saveAll(List.of(tag1, tag2, tag3, tag4));


                Product product1 = new Product();
                product1.setName("Shampoo Korea Bagus");
                product1.setDescription("Description shampoo korea bagus");
                product1.setPrice(new BigDecimal(150000));
                product1.setStock(100);
                product1.setCategory(category1);

                Product product2 = new Product();
                product2.setName("Sabun Korea Bagus");
                product2.setDescription("Description sabun korea bagus");
                product2.setPrice(new BigDecimal(100000));
                product2.setStock(100);
                product2.setCategory(category2);

                productRepository.saveAll(List.of(product1, product2));

                ProductTag productTag1 = new ProductTag();
                productTag1.setTag(tag1);
                productTag1.setProduct(product1);
                productTag1.setId(new ProductTagId(product1.getId(), tag1.getId()));
                product1.getProductTags().add(productTag1);

                ProductTag productTag2 = new ProductTag();
                productTag2.setTag(tag2);
                productTag2.setProduct(product1);
                productTag2.setId(new ProductTagId(product1.getId(), tag2.getId()));
                product1.getProductTags().add(productTag2);

                ProductTag productTag3 = new ProductTag();
                productTag3.setTag(tag3);
                productTag3.setProduct(product1);
                productTag3.setId(new ProductTagId(product1.getId(), tag3.getId()));
                product1.getProductTags().add(productTag3);

                productTag1 = new ProductTag();
                productTag1.setTag(tag1);
                productTag1.setProduct(product2);
                productTag1.setId(new ProductTagId(product2.getId(), tag1.getId()));
                product2.getProductTags().add(productTag1);

                productTag2 = new ProductTag();
                productTag2.setTag(tag2);
                productTag2.setProduct(product2);
                productTag2.setId(new ProductTagId(product2.getId(), tag2.getId()));
                product2.getProductTags().add(productTag2);

                productRepository.saveAll(List.of(product1, product2));

        }

        @Test
        @WithMockUser(roles = "USER")
        void testFindAllProducts_ReturnsList() {

                // Perform GET request
                ResponseEntity<ApiResponse<List<ProductResponse>>> response = restTemplate.exchange(
                        baseUrl,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

                // Basic check
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().success()).isTrue();
                assertThat(response.getBody().message()).isNotEmpty();
                assertThat(response.getBody().message()).isEqualTo("success");
                assertThat(response.getBody().data()).hasSize(2);
                assertThat(response.getBody().data())
                        .extracting(ProductResponse::name)
                        .containsExactlyInAnyOrder("Shampoo Korea Bagus", "Sabun Korea Bagus");
                assertThat(response.getBody().data())
                        .extracting(ProductResponse::category)
                        .containsExactlyInAnyOrder(new CategorySimpleResponse(category1.getId(), category1.getName()), new CategorySimpleResponse(category2.getId(), category2.getName()));
                // Optional: Filter example – only products in category1
                List<ProductResponse> category1Products = response.getBody().data().stream()
                        .filter(product -> product.category().id().equals(category1.getId()))
                        .toList();

                assertThat(category1Products)
                        .extracting(ProductResponse::name)
                        .containsExactly("Shampoo Korea Bagus");
        }

        @Test
        @WithMockUser(roles = "USER")
        void testFindAllProducts_WhenNoProductsExist_ReturnsEmptyList() {
                // Pastikan database kosong
                productRepository.deleteAll();

                // Lakukan GET request ke endpoint
                ResponseEntity<ApiResponse<List<ProductResponse>>> response = restTemplate.exchange(
                        baseUrl,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

                // Validasi response
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().success()).isTrue();
                assertThat(response.getBody().message()).isNotEmpty();
                assertThat(response.getBody().message()).isEqualTo("success");
                assertThat(response.getBody().data()).isNotNull();
                assertThat(response.getBody().data()).isEmpty();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void testSaveProduct_ReturnsSuccess() {
                CreateProductRequest request = new CreateProductRequest("Shampoo Korea Bagus",
                        "Ini shampo original Korea lohhh",
                        new BigDecimal(150000),
                        100,
                        category1.getId(), Set.of(tag4.getId(), tag1.getId()));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<CreateProductRequest> entity = new HttpEntity<>(request, headers);

                // Execute
                ResponseEntity<ApiResponse<ProductResponse>> response = restTemplate.exchange(
                        baseUrl,
                        HttpMethod.POST,
                        entity,
                        new ParameterizedTypeReference<>() {}
                );

                // Validasi status dan struktur response
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();

                ApiResponse<ProductResponse> body = response.getBody();
                assertThat(body.success()).isTrue();
                assertThat(body.message()).isEqualTo("success");
                assertThat(body.data()).isNotNull();

                // Validasi field product
                ProductResponse product = body.data();
                assertThat(product.name()).isEqualTo("Shampoo Korea Bagus");
                assertThat(product.price()).isEqualTo(new BigDecimal("150000"));
                assertThat(product.stock()).isEqualTo(100);
                assertThat(product.category().name()).isEqualTo("Shampoo");

                // Validasi tags
                List<String> tagNames = product.tag().stream()
                        .map(TagResponse::name)
                        .toList();

                assertThat(tagNames).containsExactlyInAnyOrder("Korea", "Shampoo");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void testSaveProduct_WhenRequestNotValid_ReturnsBadRequest() {
                // Hanya field `name` yang tidak valid, lainnya valid
                CreateProductRequest request = new CreateProductRequest(
                        "", // invalid name
                        "Deskripsi valid",
                        BigDecimal.valueOf(100000),
                        10,
                        UUID.randomUUID(),
                        Set.of(UUID.randomUUID())
                );

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<CreateProductRequest> entity = new HttpEntity<>(request, headers);

                // Execute
                ResponseEntity<ApiResponse<ProductResponse>> response = restTemplate.exchange(
                        baseUrl,
                        HttpMethod.POST,
                        entity,
                        new ParameterizedTypeReference<>() {}
                );

                // Validasi response
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().success()).isFalse();
                assertThat(response.getBody().message()).isNotEmpty();
                assertThat(response.getBody().message()).isEqualTo("name: Product name is required");
                assertThat(response.getBody().data()).isNull();
        }
}
