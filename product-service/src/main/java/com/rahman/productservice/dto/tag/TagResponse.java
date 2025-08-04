package com.rahman.productservice.dto.tag;

import java.io.Serializable;
import java.util.UUID;

public record TagResponse(
        UUID id,
        String name
) implements Serializable {
}
