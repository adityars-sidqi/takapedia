package com.rahman.productservice.dto.tag;

import java.util.UUID;

public record TagResponse(
        UUID id,
        String name
) {
}
