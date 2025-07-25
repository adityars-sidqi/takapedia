package com.rahman.productservice.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCategoryRequest(
        @NotBlank(message = "{NotBlank.category.name}")
        @Size(max = 100, message = "{Size.category.name}")
        String name,
        String description,
        UUID parentId)
{}
