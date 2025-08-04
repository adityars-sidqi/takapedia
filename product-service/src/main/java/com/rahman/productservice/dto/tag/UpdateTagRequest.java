package com.rahman.productservice.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTagRequest(
        @NotBlank(message = "{tag.name.not_blank}")
        @NotNull
        String name)
{}
