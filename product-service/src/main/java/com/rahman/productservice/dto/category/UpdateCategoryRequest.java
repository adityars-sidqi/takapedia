package com.rahman.productservice.dto.category;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateCategoryRequest(
        @Size(min = 1, max = 100, message = "{category.name.size}")
        String name,
        @Size(min = 1, message = "{category.description.size}")
        String description,
        UUID parentId)
{}
