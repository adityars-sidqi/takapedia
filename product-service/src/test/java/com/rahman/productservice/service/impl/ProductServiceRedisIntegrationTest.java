package com.rahman.productservice.service.impl;

import com.rahman.productservice.dto.product.CreateProductRequest;
import com.rahman.productservice.dto.product.ProductResponse;
import com.rahman.productservice.dto.product.UpdateProductRequest;
import com.rahman.productservice.entity.Category;
import com.rahman.productservice.entity.Product;
import com.rahman.productservice.entity.ProductStatus;
import com.rahman.productservice.entity.Tag;
import com.rahman.productservice.repository.CategoryRepository;
import com.rahman.productservice.repository.ProductRepository;
import com.rahman.productservice.repository.TagRepository;
import com.rahman.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductServiceRedisIntegrationTest extends BaseServiceIntegrationTest{

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    private Category savedCategory;
    private Tag savedTag;

    @BeforeEach
    void setup() {
        categoryRepository.deleteAll();
        tagRepository.deleteAll();
        productRepository.deleteAll();

        Category category = new Category();
        category.setName("Electronics");
        savedCategory = categoryRepository.save(category);

        Tag tag = new Tag();
        tag.setName("Gadget");
        savedTag = tagRepository.save(tag);
    }

    @Test
    void testFindAll_ShouldCacheResult() {
        // given
        Product product = new Product();
        product.setName("Laptop");
        product.setDescription("Gaming laptop");
        product.setPrice(BigDecimal.valueOf(1500));
        product.setStock(10);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(savedCategory);

        productRepository.save(product);

        // when: first call -> DB hit
        List<ProductResponse> firstCall = productService.findAll();
        // then
        assertThat(firstCall).hasSize(1);

        // when: second call -> should return from cache
        productRepository.deleteAll(); // hapus dari DB untuk memastikan cache digunakan
        List<ProductResponse> secondCall = productService.findAll();

        // then: tetap ada data meskipun DB kosong (artinya cache jalan)
        assertThat(secondCall).hasSize(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSave_ShouldPersistEntity_AndEvictCache() {
        // given
        CreateProductRequest request = new CreateProductRequest(
                "Smartphone", "Android phone",
                BigDecimal.valueOf(500), 20, ProductStatus.ACTIVE,
                savedCategory.getId(),
                Set.of(savedTag.getId())
        );

        // when
        ProductResponse saved = productService.save(request);

        // then
        assertThat(saved).isNotNull();
        assertThat(productRepository.findById(saved.id())).isPresent();

        // and cache should be evicted, new data masuk di findAll
        List<ProductResponse> products = productService.findAll();
        assertThat(products).extracting(ProductResponse::name)
                .contains("Smartphone");

    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdate_ShouldEvictCacheAndUpdateData() {
        // given
        Product product = new Product();
        product.setName("Tablet");
        product.setDescription("Old tablet");
        product.setPrice(BigDecimal.valueOf(200));
        product.setStatus(ProductStatus.ACTIVE);
        product.setStock(5);
        product.setCategory(savedCategory);

        Product savedProduct = productRepository.save(product);

        UpdateProductRequest updateRequest = new UpdateProductRequest(
                "Tablet Pro", "Updated tablet", BigDecimal.valueOf(300), 8,
                savedCategory.getId(), Set.of(savedTag.getId())
        );

        // when
        ProductResponse updated = productService.update(savedProduct.getId(), updateRequest);

        // then
        assertThat(updated.name()).isEqualTo("Tablet Pro");
        assertThat(updated.price()).isEqualTo(BigDecimal.valueOf(300));

        // cache eviction check: langsung ambil dari service
        List<ProductResponse> products = productService.findAll();
        assertThat(products).extracting(ProductResponse::name)
                .contains("Tablet Pro");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDelete_ShouldRemoveEntity_AndEvictCache() {
        // given
        Product product = new Product();
        product.setName("Headphone");
        product.setDescription("Wireless headphone");
        product.setPrice(BigDecimal.valueOf(100));
        product.setStatus(ProductStatus.ACTIVE);
        product.setStock(15);
        product.setCategory(savedCategory);

        Product savedProduct = productRepository.save(product);

        UUID id = savedProduct.getId();

        // when
        productService.deleteById(id);

        // then
        assertThat(productRepository.findById(id)).isEmpty();

        // cache eviction check
        List<ProductResponse> products = productService.findAll();
        assertThat(products)
                .noneMatch(p -> p.id().equals(id));

    }

}
