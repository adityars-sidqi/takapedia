package com.rahman.productservice.dto.category;

import java.util.UUID;

public record CategorySimpleResponse(
        UUID id,
        String name) {
}
