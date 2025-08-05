package com.rahman.productservice.dto.product;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * Request DTO for creating a new Product.
 * <p>
 * This DTO contains all necessary fields required to create a product,
 * including basic product info, category, and associated tags.
 *
 * <ul>
 *     <li><b>name</b>: Product name, required and max 200 characters.</li>
 *     <li><b>description</b>: Product description, required and max 65535 characters.</li>
 *     <li><b>price</b>: Product price, must be a positive decimal (max 2 fraction digits).</li>
 *     <li><b>stock</b>: Product stock, must be a non-negative integer.</li>
 *     <li><b>categoryId</b>: UUID of the product's category, required.</li>
 *     <li><b>tagIds</b>: Optional set of tag UUIDs associated with the product.</li>
 * </ul>
 *
 * Validation messages are defined via i18n keys such as <code>product.name.not_blank</code>, etc.
 *
 * @author Aditya Rahman Sidqi
 */
public record CreateProductRequest(
        @NotBlank(message = "{product.name.not_blank}")
        @Size(max = 200, message = "{product.name.size}")
        String name,

        @NotBlank(message = "{product.description.not_blank}")
        @Size(max = 65535, message = "{product.description.size}")
        String description,

        @NotNull(message = "{product.price.not_null}")
        @DecimalMin(value = "0.0", inclusive = true, message = "{product.price.min}")
        @Digits(integer = 10, fraction = 2, message = "{product.price.format}")
        BigDecimal price,

        @NotNull(message = "{product.stock.not_null}")
        @Min(value = 0, message = "{product.stock.min}")
        Integer stock,

        @NotNull(message = "{product.category_id.not_null}")
        UUID categoryId,

        Set<UUID> tagIds

) {
}
