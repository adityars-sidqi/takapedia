package com.rahman.productservice.service.impl;

import com.rahman.productservice.dto.category.CategoryResponse;
import com.rahman.productservice.dto.category.CreateCategoryRequest;
import com.rahman.productservice.dto.category.UpdateCategoryRequest;
import com.rahman.productservice.entity.Category;
import com.rahman.productservice.repository.CategoryRepository;
import com.rahman.productservice.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryServiceRedisIntegrationTest extends BaseServiceIntegrationTest {

    private static final String CATEGORY_CACHE = "categories";

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setup() {
        categoryRepository.deleteAll();
    }

    @Test
    @Transactional
    void testFindAll_ShouldCacheResult() {
        // given
        Category category = new Category();
        category.setName("Toys");
        categoryRepository.save(category);

        // when (first call - hits DB)
        List<CategoryResponse> result1 = categoryService.findAll();

        // then
        assertThat(result1).hasSize(1);
        assertThat(result1.getFirst().name()).isEqualTo("Toys");

        // when (second call - should come from cache)
        List<CategoryResponse> result2 = categoryService.findAll();

        // then: same result & cache must be present
        assertThat(result2).hasSize(1);
        assertCachePresent(CATEGORY_CACHE, SimpleKey.EMPTY);
    }

    @Test
    void testSave_ShouldPersistEntity_AndEvictCache() {
        // given
        categoryService.findAll(); // prime cache

        // when
        CreateCategoryRequest request = new CreateCategoryRequest("Hobbies", "Hobby and Toys", null);
        CategoryResponse saved = categoryService.save(request);

        // then
        assertThat(categoryRepository.findById(saved.id())).isPresent();
        assertCacheEvicted(CATEGORY_CACHE, SimpleKey.EMPTY);
    }

    @Test
    @Transactional
    void testUpdate_ShouldEvictCacheAndUpdateData() {
        // given
        Category category = new Category();
        category.setName("Gadgets");
        Category saved = categoryRepository.save(category);

        categoryService.findAll(); // prime cache

        // when
        UpdateCategoryRequest updateRequest = new UpdateCategoryRequest("Updated Gadgets", "Updated Gadgets", null);
        CategoryResponse updated = categoryService.update(saved.getId(), updateRequest);

        // then
        assertThat(updated.name()).isEqualTo("Updated Gadgets");
        assertThat(categoryRepository.findById(saved.getId())
                .get()
                .getName()
        ).isEqualTo("Updated Gadgets");

        // cache should be evicted
        assertCacheEvicted(CATEGORY_CACHE, SimpleKey.EMPTY);
    }

    @Test
    @Transactional
    void testDelete_ShouldRemoveEntity_AndEvictCache() {
        // given
        Category category = new Category();
        category.setName("Clothes");
        Category saved = categoryRepository.save(category);

        categoryService.findAll(); // prime cache

        UUID id = saved.getId();

        // when
        categoryService.deleteById(id);

        // then
        assertThat(categoryRepository.findById(id)).isEmpty();
        assertCacheEvicted(CATEGORY_CACHE, SimpleKey.EMPTY);
    }

}
