package com.rahman.productservice.dto.product;

import com.rahman.productservice.dto.category.CategorySimpleResponse;
import com.rahman.productservice.dto.tag.TagResponse;

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
        CategorySimpleResponse category,
        List<TagResponse> tag,
        Instant createdAt,
        Instant updatedAt) {
}
