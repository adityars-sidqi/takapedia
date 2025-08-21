package com.rahman.productservice.dto.product;

import com.rahman.productservice.dto.category.CategorySimpleResponse;
import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        ProductStatus status,
        CategorySimpleResponse category,
        List<TagResponse> tags,
        Instant createdAt,
        Instant updatedAt) {
}
