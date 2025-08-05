package com.rahman.productservice.mapper;

import com.rahman.productservice.dto.category.CategorySimpleResponse;
import com.rahman.productservice.dto.product.CreateProductRequest;
import com.rahman.productservice.dto.product.ProductResponse;
import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.entity.Category;
import com.rahman.productservice.entity.Product;
import com.rahman.productservice.entity.ProductTag;
import com.rahman.productservice.entity.Tag;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    default ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                new CategorySimpleResponse(
                        product.getCategory().getId(),
                        product.getCategory().getName()
                ),
                product.getProductTags().stream()
                        .map(productTag -> new TagResponse(
                                productTag.getTag().getId(),
                                productTag.getTag().getName()
                        ))
                        .toList(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    default Product toEntity(CreateProductRequest request, Category category, List<Tag> tags) {
        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(category);

        if (tags != null) {
            product.setProductTags(tags.stream()
                    .map(tag -> new ProductTag(product, tag))
                    .collect(Collectors.toSet()));
        }

        return product;
    }
}
