package com.rahman.productservice.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCategoryRequest(
        @NotBlank(message = "{category.name.not_blank}")
        @Size(max = 100, message = "{category.name.size}")
        String name,
        String description,
        UUID parentId)
{}
