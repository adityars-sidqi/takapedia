package com.rahman.productservice.controller;


import com.rahman.commonlib.ApiResponse;
import com.rahman.productservice.ProductServiceApplication;
import com.rahman.productservice.dto.category.CategoryResponse;
import com.rahman.productservice.dto.category.CreateCategoryRequest;
import com.rahman.productservice.dto.category.UpdateCategoryRequest;
import com.rahman.productservice.entity.Category;
import com.rahman.productservice.repository.CategoryRepository;
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

import java.util.List;
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
class CategoryControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CategoryRepository categoryRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/category";

        // Prepare test data
        Category category1 = new Category();
        category1.setName("Electronics");

        Category category2 = new Category();
        category2.setName("Hobbies");

        List<Category> categories = List.of(category1, category2);
        categoryRepository.saveAll(categories);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindAllCategories_ReturnsList() {

        // Perform GET request
        ResponseEntity<ApiResponse<List<CategoryResponse>>> response = restTemplate.exchange(
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
                .extracting(CategoryResponse::name)
                .containsExactlyInAnyOrder("Electronics", "Hobbies");
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindAllCategories_WhenNoCategoriesExist_ReturnsEmptyList() {
        // Pastikan database kosong
        categoryRepository.deleteAll();

        // Lakukan GET request ke endpoint
        ResponseEntity<ApiResponse<List<CategoryResponse>>> response = restTemplate.exchange(
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
    void testSaveCategory_ReturnsSuccess() {
        CreateCategoryRequest request = new CreateCategoryRequest("Finance", "Financial", null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CreateCategoryRequest> entity = new HttpEntity<>(request, headers);

        // Execute
        ResponseEntity<ApiResponse<CategoryResponse>> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        // Validasi response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isNotEmpty();
        assertThat(response.getBody().message()).isEqualTo("success");
        assertThat(response.getBody().data()).isNotNull();
        assertThat(response.getBody().data().name()).isEqualTo("Finance");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSaveCategory_WhenNameIsBlank_ReturnsBadRequest() {
        CreateCategoryRequest request = new CreateCategoryRequest("", "Financial", null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CreateCategoryRequest> entity = new HttpEntity<>(request, headers);

        // Execute
        ResponseEntity<ApiResponse<CategoryResponse>> response = restTemplate.exchange(
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
        assertThat(response.getBody().message()).isEqualTo("name: Category name is required");
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteCategory_Success() {

        //Prepare data
        Category category = new Category();
        category.setName("Top Up & Tagihan");

        Category categorySaved = categoryRepository.save(category);
        UUID id = categorySaved.getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity entity = new HttpEntity<>(headers);

        // Execute
        ResponseEntity<ApiResponse<CategoryResponse>> response = restTemplate.exchange(
                baseUrl + "/" +  id,
                HttpMethod.DELETE,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        // Validasi response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isNotEmpty();
        assertThat(response.getBody().message()).isEqualTo("success");
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteCategory_NotFound() {

        UUID id = UUID.randomUUID();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity entity = new HttpEntity<>(headers);

        // Execute
        ResponseEntity<ApiResponse<CategoryResponse>> response = restTemplate.exchange(
                baseUrl + "/" +  id,
                HttpMethod.DELETE,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        // Validasi response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isNotEmpty();
        assertThat(response.getBody().message()).isEqualTo("Category not found.");
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateCategory_ReturnsSuccess() {

        //Prepare data
        Category category = new Category();
        category.setName("Electronics");
        Category categorySaved = categoryRepository.save(category);
        UUID id = categorySaved.getId();

        UpdateCategoryRequest request = new UpdateCategoryRequest("Hobbies", "Hobby and Toys", null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<UpdateCategoryRequest> entity = new HttpEntity<>(request, headers);

        // Execute
        ResponseEntity<ApiResponse<CategoryResponse>> response = restTemplate.exchange(
                baseUrl + "/" +  id,
                HttpMethod.PATCH,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        // Validasi response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isNotEmpty();
        assertThat(response.getBody().message()).isEqualTo("success");
        assertThat(response.getBody().data()).isNotNull();
        assertThat(response.getBody().data().name()).isEqualTo("Hobbies");
        assertThat(response.getBody().data().description()).isEqualTo("Hobby and Toys");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateCategory_WhenNameIsBlank_ReturnsBadRequest() {
        //Prepare data
        Category category = new Category();
        category.setName("Makeup");
        Category categorySaved = categoryRepository.save(category);
        UUID id = categorySaved.getId();

        UpdateCategoryRequest request = new UpdateCategoryRequest("", null, null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<UpdateCategoryRequest> entity = new HttpEntity<>(request, headers);

        // Execute
        ResponseEntity<ApiResponse<CategoryResponse>> response = restTemplate.exchange(
                baseUrl + "/" +  id,
                HttpMethod.PATCH,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        // Validasi response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isNotEmpty();
        assertThat(response.getBody().message()).contains("name: Category name is required");
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateCategory_NotFound() {

        UUID id = UUID.randomUUID();

        UpdateCategoryRequest request = new UpdateCategoryRequest("Hobbies", "Hobby and Toys", null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<UpdateCategoryRequest> entity = new HttpEntity<>(request, headers);

        // Execute
        ResponseEntity<ApiResponse<CategoryResponse>> response = restTemplate.exchange(
                baseUrl + "/" +  id,
                HttpMethod.PATCH,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        // Validasi response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isNotEmpty();
        assertThat(response.getBody().message()).isEqualTo("Category not found.");
        assertThat(response.getBody().data()).isNull();
    }
}
