package com.rahman.productservice.controller;

import com.rahman.commonlib.ApiResponse;
import com.rahman.productservice.ProductServiceApplication;
import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.entity.Tag;
import com.rahman.productservice.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ProductServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.fail-fast=false"
        }
)
@ActiveProfiles("test")
class TagControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TagRepository tagRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/tag";

        // Prepare test data
        Tag tag1 = new Tag();
        tag1.setName("Makeup");

        Tag tag2 = new Tag();
        tag2.setName("Discount");

        List<Tag> tags = List.of(tag1, tag2);
        tagRepository.saveAll(tags);
    }


    @Test
    @WithMockUser(roles = "USER")
    void testFindAllTags_ReturnsList() {

        // Perform GET request
        ResponseEntity<ApiResponse<List<TagResponse>>> response = restTemplate.exchange(
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
                .extracting(TagResponse::name)
                .containsExactlyInAnyOrder("Makeup", "Discount");
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindAllTags_WhenNoTagsExist_ReturnsEmptyList() {
        // Pastikan database kosong
        tagRepository.deleteAll();

        // Lakukan GET request ke endpoint
        ResponseEntity<ApiResponse<List<TagResponse>>> response = restTemplate.exchange(
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
}