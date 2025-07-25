package com.rahman.productservice.service;

import com.rahman.productservice.dto.category.CategoryResponse;
import com.rahman.productservice.dto.category.CreateCategoryRequest;
import com.rahman.productservice.dto.category.UpdateCategoryRequest;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    List<CategoryResponse>  findAll();
    CategoryResponse findById(UUID id);
    CategoryResponse save(CreateCategoryRequest  createCategoryRequest);
    CategoryResponse update(UUID id, UpdateCategoryRequest updateCategoryRequest);
    void deleteById(UUID id);
}
