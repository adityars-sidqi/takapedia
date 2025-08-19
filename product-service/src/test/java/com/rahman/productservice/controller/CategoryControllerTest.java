package com.rahman.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rahman.productservice.dto.category.CategorySimpleResponse;
import com.rahman.productservice.dto.category.CreateCategoryRequest;
import com.rahman.productservice.dto.category.CategoryResponse;
import com.rahman.productservice.dto.category.UpdateCategoryRequest;
import com.rahman.productservice.entity.Category;
import com.rahman.productservice.exception.ResourceNotFoundException;
import com.rahman.productservice.mapper.CategoryMapper;
import com.rahman.productservice.service.CategoryService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private MessageSource messageSource;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindAll_ReturnsListOfCategories() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        CategoryResponse category = new CategoryResponse(id, "Apple", "Apple",
                new CategorySimpleResponse(UUID.randomUUID(), "Electronics"),
                Instant.now(), Instant.now(), null);
        when(categoryService.findAll()).thenReturn(List.of(category));

        // Act & Assert
        mockMvc.perform(get("/category")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("success")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(id.toString())))
                .andExpect(jsonPath("$.data[0].name", is("Apple")))
                .andExpect(jsonPath("$.data[0].parent.name", is("Electronics")));

        verify(categoryService).findAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSave_ReturnsSuccess() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Electronics", "Electronics", null);
        Category categoryData = new Category();
        categoryData.setId(UUID.randomUUID());
        categoryData.setName("Makeup");

        when(categoryService.save(any(CreateCategoryRequest.class))).thenReturn(categoryMapper.toResponse(categoryData));

        mockMvc.perform(post("/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("success")))
                .andExpect(jsonPath("$.data.name", is("Makeup")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDelete_ReturnSuccess() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/category/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("success")))
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()));

        verify(categoryService).deleteById(id);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDelete_WhenIdNotFound_ReturnNotFound() throws Exception {
        UUID id = UUID.randomUUID();

        doThrow(new ResourceNotFoundException(messageSource.getMessage("category.not_found", null, LocaleContextHolder.getLocale())))
                .when(categoryService).deleteById(any(UUID.class));

        mockMvc.perform(delete("/category/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Category not found.")))
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()));

        verify(categoryService).deleteById(id);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdate_ReturnSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateCategoryRequest request = new UpdateCategoryRequest("Electronics", "Electronics", null);
        Category categoryData = new Category();
        categoryData.setId(id);
        categoryData.setName("Makeup");

        when(categoryService.update(any(UUID.class), any(UpdateCategoryRequest.class))).thenReturn(categoryMapper.toResponse(categoryData));

        mockMvc.perform(patch("/category/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("success")))
                .andExpect(jsonPath("$.data.id", is(id.toString())))
                .andExpect(jsonPath("$.data.name", is("Makeup")));

    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdate_WhenIdNotFound_ReturnNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateCategoryRequest request = new UpdateCategoryRequest("Electronics", "Electronics", null);

        doThrow(new ResourceNotFoundException(messageSource.getMessage("category.not_found", null, LocaleContextHolder.getLocale())))
                .when(categoryService).update(any(UUID.class), any(UpdateCategoryRequest.class));

        mockMvc.perform(patch("/category/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Category not found.")))
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()));

        verify(categoryService).update(id, request);
    }
}
