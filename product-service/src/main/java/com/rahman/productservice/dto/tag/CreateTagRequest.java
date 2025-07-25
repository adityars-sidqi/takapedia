package com.rahman.productservice.dto.tag;

import jakarta.validation.constraints.NotBlank;

public record CreateTagRequest(
        @NotBlank(message = "{tag.name.not_blank}")
        String name
) {}
