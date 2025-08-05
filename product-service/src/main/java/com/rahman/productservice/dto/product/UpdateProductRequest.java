package com.rahman.productservice.dto.product;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record UpdateProductRequest(

        String name,
        String description,
        BigDecimal price,
        Integer stock,
        UUID categoryId,
        Set<UUID> tagIds
) {
}
