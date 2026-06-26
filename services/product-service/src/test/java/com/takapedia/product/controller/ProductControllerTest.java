package com.takapedia.product.controller;

import com.takapedia.product.dto.CreateProductRequest;
import com.takapedia.product.dto.ProductResponse;
import com.takapedia.product.dto.UpdateProductRequest;
import com.takapedia.product.entity.Product;
import com.takapedia.product.exception.ProductNotFoundException;
import com.takapedia.product.security.JwtService;
import com.takapedia.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getById_whenProductExists_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Product product = new Product(
                id, "Laptop", "Gaming laptop",
                new BigDecimal("15000000.00"), Instant.now()
        );
        when(productService.getById(id)).thenReturn(product);

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(15000000.00));
    }

    @Test
    void getById_whenProductNotFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getById(id)).thenThrow(new ProductNotFoundException(id));

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void add_whenValidRequest_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest(
                "Laptop", "Gaming laptop", new BigDecimal("15000000.00")
        );
        Product saved = new Product(
                id, "Laptop", "Gaming laptop",
                new BigDecimal("15000000.00"), Instant.now()
        );
        when(productService.add(any(CreateProductRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void add_whenInvalidRequest_returns400() throws Exception {
        CreateProductRequest invalid = new CreateProductRequest(
                "", "Gaming laptop", new BigDecimal("-100")
        );

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_whenValidRequest_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateProductRequest request = new UpdateProductRequest(
                "Laptop Pro", "Updated", new BigDecimal("20000000.00")
        );
        Product updated = new Product(
                id, "Laptop Pro", "Updated",
                new BigDecimal("20000000.00"), Instant.now()
        );
        when(productService.update(eq(id), any(UpdateProductRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop Pro"));
    }

    @Test
    void update_whenProductNotFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateProductRequest request = new UpdateProductRequest(
                "Laptop Pro", "Updated", new BigDecimal("20000000.00")
        );
        when(productService.update(eq(id), any(UpdateProductRequest.class)))
                .thenThrow(new ProductNotFoundException(id));

        mockMvc.perform(put("/api/v1/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/products/{id}", id))
                .andExpect(status().isNoContent());

        verify(productService).delete(id);
    }
}