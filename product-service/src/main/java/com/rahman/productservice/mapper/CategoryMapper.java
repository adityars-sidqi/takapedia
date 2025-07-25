package com.rahman.productservice.mapper;

import com.rahman.productservice.dto.category.CategoryResponse;
import com.rahman.productservice.dto.category.CategorySimpleResponse;
import com.rahman.productservice.dto.category.CreateCategoryRequest;
import com.rahman.productservice.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    default CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getParent() != null
                        ? new CategorySimpleResponse(category.getParent().getId(), category.getParent().getName())
                        : null,
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.getCategories() != null
                        ? category.getCategories().stream()
                        .map(child -> new CategorySimpleResponse(child.getId(), child.getName()))
                        .collect(Collectors.toSet())
                        : Set.of()
        );
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category mapToEntity(CreateCategoryRequest request);
}
