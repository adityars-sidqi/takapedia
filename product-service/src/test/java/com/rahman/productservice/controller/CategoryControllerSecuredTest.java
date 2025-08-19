package com.rahman.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rahman.productservice.ProductServiceApplication;
import com.rahman.productservice.dto.category.CreateCategoryRequest;
import com.rahman.productservice.dto.category.UpdateCategoryRequest;
import com.rahman.productservice.entity.Category;
import com.rahman.productservice.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class CategoryControllerSecuredTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void testSaveCategory_WhenRoleIsUser_ReturnsBadRequest() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Electronics", "Electronics", null);

        mockMvc.perform(post("/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSaveCategory_WhenNotAuthenticated_ReturnsUnauthorized() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Electronics", "Electronics", null);

        mockMvc.perform(post("/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void testDeleteCategory_WhenRoleIsUser_ReturnsBadRequest() throws Exception {
        //Prepare data
        Category category = new Category();
        category.setName("Makeup");
        Category categorySaved = categoryRepository.save(category);
        UUID id = categorySaved.getId();


        mockMvc.perform(delete("/category/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteCategory_WhenNotAuthenticated_ReturnsUnauthorized() throws Exception {
        //Prepare data
        Category category = new Category();
        category.setName("Makeup");
        Category categorySaved = categoryRepository.save(category);
        UUID id = categorySaved.getId();


        mockMvc.perform(delete("/category/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void testUpdateCategory_WhenRoleIsUser_ReturnsBadRequest() throws Exception {
        //Prepare data
        Category category = new Category();
        category.setName("Makeup");
        Category categorySaved = categoryRepository.save(category);
        UUID id = categorySaved.getId();

        UpdateCategoryRequest request = new UpdateCategoryRequest("Electronics", "Electronics", null);


        mockMvc.perform(patch("/category/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateCategory_WhenNotAuthenticated_ReturnsUnauthorized() throws Exception {
        //Prepare data
        Category category = new Category();
        category.setName("Makeup");
        Category categorySaved = categoryRepository.save(category);
        UUID id = categorySaved.getId();


        UpdateCategoryRequest request = new UpdateCategoryRequest("Electronics", "Electronics", null);


        mockMvc.perform(patch("/category/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
