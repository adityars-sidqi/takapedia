package com.rahman.productservice.service.impl;

import com.rahman.commonlib.exception.BadRequestException;
import com.rahman.commonlib.exception.ResourceNotFoundException;
import com.rahman.productservice.dto.category.CategoryResponse;
import com.rahman.productservice.dto.category.CreateCategoryRequest;
import com.rahman.productservice.dto.category.UpdateCategoryRequest;
import com.rahman.productservice.entity.Category;
import com.rahman.productservice.mapper.CategoryMapper;
import com.rahman.productservice.repository.CategoryRepository;
import com.rahman.productservice.service.CategoryService;
import com.rahman.productservice.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ValidationService validationService;
    private final CategoryMapper categoryMapper;
    private final MessageSource messageSource;

    @Autowired
    public CategoryServiceImpl(CategoryRepository categoryRepository, ValidationService validationService,
                               CategoryMapper categoryMapper, MessageSource messageSource) {
        this.categoryRepository = categoryRepository;
        this.validationService = validationService;
        this.categoryMapper = categoryMapper;
        this.messageSource = messageSource;
    }

    @Override
    public List<CategoryResponse> findAll() {

        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse findById(UUID id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        return categoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse save(CreateCategoryRequest createCategoryRequest) {
        validationService.validate(createCategoryRequest);

        Category category = categoryMapper.mapToEntity(createCategoryRequest);

        if (createCategoryRequest.parentId() != null) {
            Category parentCategory = categoryRepository.findById(createCategoryRequest.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException(messageSource.getMessage("category.not_found", null, LocaleContextHolder.getLocale()))); // gunakan i18n key

            category.setParent(parentCategory);
        }

        Category categorySaved = categoryRepository.save(category);

        return categoryMapper.toResponse(categorySaved);
    }

    @Override
    public CategoryResponse update(UUID id, UpdateCategoryRequest updateCategoryRequest) {
        validationService.validate(updateCategoryRequest);

        Category category = categoryRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException(
                        messageSource.getMessage("category.not_found", null, LocaleContextHolder.getLocale())
                ));

        // Update name jika tidak null dan tidak kosong
        Optional.ofNullable(updateCategoryRequest.name())
                .filter(name -> !name.isBlank())
                .ifPresent(category::setName);

        // Update description jika tidak null dan tidak kosong
        Optional.ofNullable(updateCategoryRequest.description())
                .filter(desc -> !desc.isBlank())
                .ifPresent(category::setDescription);

        if (updateCategoryRequest.parentId() != null) {
            if (updateCategoryRequest.parentId().equals(id)) {
                throw new BadRequestException("Category cannot be its own parent");
            }

            Category parentCategory = categoryRepository.findById(updateCategoryRequest.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category not found"));

            category.setParent(parentCategory);
        }


        Category updated  = categoryRepository.save(category);

        return categoryMapper.toResponse(updated);
    }

    @Override
    public void deleteById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                messageSource.getMessage("category.not_found", null, LocaleContextHolder.getLocale())
                        ));

        categoryRepository.delete(category);
    }
}
