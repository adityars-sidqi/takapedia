package com.rahman.productservice.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UpdateProductRequest(
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        UUID categoryId,
        Set<UUID> tagIds
) {
}
