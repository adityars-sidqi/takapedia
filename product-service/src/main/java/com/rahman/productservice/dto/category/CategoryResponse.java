package com.rahman.productservice.dto.category;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for {@link com.rahman.productservice.entity.Category}
 */
public record CategoryResponse(
        UUID id,
        String name,
        String description,
        CategorySimpleResponse parent,
        Instant createdAt,
        Instant updatedAt,
        Set<CategorySimpleResponse> subCategories
) implements Serializable {
}