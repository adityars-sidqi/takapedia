package com.rahman.productservice.controller;

import com.rahman.commonlib.ApiResponse;
import com.rahman.productservice.dto.category.CategoryResponse;
import com.rahman.productservice.dto.category.CreateCategoryRequest;
import com.rahman.productservice.dto.category.UpdateCategoryRequest;
import com.rahman.productservice.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/category")
class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<List<CategoryResponse>>  findAll() {
        return ApiResponse.success(categoryService.findAll());
    }

    @GetMapping(value="/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<CategoryResponse> findById(@PathVariable("id") UUID id) {
        return ApiResponse.success(categoryService.findById(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> save(@RequestBody CreateCategoryRequest createCategoryRequest) {
        return ApiResponse.success(categoryService.save(createCategoryRequest));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> update(@PathVariable("id") UUID id, @RequestBody UpdateCategoryRequest updateCategoryRequest) {
        return ApiResponse.success(categoryService.update(id, updateCategoryRequest));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable("id") UUID id) {
        categoryService.deleteById(id);
        return ApiResponse.success(null);
    }


}
