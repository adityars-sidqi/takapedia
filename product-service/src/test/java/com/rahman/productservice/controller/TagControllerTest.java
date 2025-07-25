package com.rahman.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rahman.productservice.dto.tag.CreateTagRequest;
import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.entity.Tag;
import com.rahman.productservice.mapper.TagMapper;
import com.rahman.productservice.service.TagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.fail-fast=false"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagService tagService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TagMapper tagMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindAll_ReturnsListOfTags() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        TagResponse tag = new TagResponse(id, "Elektronik");
        when(tagService.findAll()).thenReturn(List.of(tag));

        // Act & Assert
        mockMvc.perform(get("/tag")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("success")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(id.toString())))
                .andExpect(jsonPath("$.data[0].name", is("Elektronik")));

        verify(tagService).findAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSave_ReturnsSuccess() throws Exception {
        CreateTagRequest request = new CreateTagRequest("Makeup");
        Tag tagData = new Tag();
        tagData.setId(UUID.randomUUID());
        tagData.setName("Makeup");

        when(tagService.save(any(CreateTagRequest.class))).thenReturn(tagMapper.toResponse(tagData));

        mockMvc.perform(post("/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("success")))
                .andExpect(jsonPath("$.data.name", is("Makeup")));
    }



}