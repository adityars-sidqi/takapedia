package com.takapedia.product.service;

import com.takapedia.product.dto.CreateProductRequest;
import com.takapedia.product.dto.UpdateProductRequest;
import com.takapedia.product.entity.Product;
import com.takapedia.product.exception.ProductNotFoundException;
import com.takapedia.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getById_whenProductNotFound_throwsProductNotFoundException() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.getById(id))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getById_whenProductExists_returnsProduct() {
        // Arrange
        UUID id = UUID.randomUUID();
        Product product = new Product(
                id, "Laptop", "Gaming laptop",
                new BigDecimal("15000000.00"), Instant.now()
        );
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        // Act
        Product result = productService.getById(id);

        // Assert
        assertThat(result).isEqualTo(product);
        assertThat(result.getName()).isEqualTo("Laptop");
    }

    @Test
    void add_whenValidRequest_savesAndReturnsProduct() {
        // Arrange
        CreateProductRequest request = new CreateProductRequest(
                "Laptop", "Gaming laptop", new BigDecimal("15000000.00")
        );
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Product result = productService.add(request);

        // Assert
        assertThat(result.getName()).isEqualTo("Laptop");
        assertThat(result.getDescription()).isEqualTo("Gaming laptop");
        assertThat(result.getPrice()).isEqualByComparingTo("15000000.00");
        assertThat(result.getCreatedAt()).isNotNull();

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Laptop");
    }

    @Test
    void update_whenProductExists_updatesAndReturnsProduct() {
        // Arrange
        UUID id = UUID.randomUUID();
        Product existing = new Product(
                id, "Laptop", "Old description",
                new BigDecimal("15000000.00"), Instant.now()
        );
        UpdateProductRequest request = new UpdateProductRequest(
                "Laptop Pro", "New description", new BigDecimal("20000000.00")
        );
        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Product result = productService.update(id, request);

        // Assert
        assertThat(result.getName()).isEqualTo("Laptop Pro");
        assertThat(result.getDescription()).isEqualTo("New description");
        assertThat(result.getPrice()).isEqualByComparingTo("20000000.00");
    }

    @Test
    void update_whenProductNotFound_throwsProductNotFoundException() {
        // Arrange
        UUID id = UUID.randomUUID();
        UpdateProductRequest request = new UpdateProductRequest(
                "Laptop Pro", "New description", new BigDecimal("20000000.00")
        );
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.update(id, request))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void delete_whenProductExists_callsRepositoryDelete() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        productService.delete(id);

        // Assert
        verify(productRepository).deleteById(id);
    }
}