package com.rahman.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rahman.productservice.ProductServiceApplication;
import com.rahman.productservice.dto.tag.CreateTagRequest;
import com.rahman.productservice.entity.Tag;
import com.rahman.productservice.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
class TagControllerSecuredTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    void setUp() {
        tagRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void testSaveTag_WhenRoleIsUser_ReturnsBadRequest() throws Exception {
        CreateTagRequest request = new CreateTagRequest("Makeup");

        mockMvc.perform(post("/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSaveTag_WhenNotAuthenticated_ReturnsUnauthorized() throws Exception {
        CreateTagRequest request = new CreateTagRequest("Makeup");

        mockMvc.perform(post("/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void testDeleteTag_WhenRoleIsUser_ReturnsBadRequest() throws Exception {
        //Prepare data
        Tag tag = new Tag();
        tag.setName("Makeup");
        Tag tagSaved = tagRepository.save(tag);
        UUID id = tagSaved.getId();


        mockMvc.perform(delete("/tag/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteTag_WhenNotAuthenticated_ReturnsUnauthorized() throws Exception {
        //Prepare data
        Tag tag = new Tag();
        tag.setName("Makeup");
        Tag tagSaved = tagRepository.save(tag);
        UUID id = tagSaved.getId();


        mockMvc.perform(delete("/tag/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }


}
