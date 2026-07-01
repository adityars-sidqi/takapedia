package com.takapedia.productsearch.dto;

import com.takapedia.productsearch.document.ProductDocument;

public record ProductDetailResponse(
        String id,
        String name,
        String description,
        Long price,
        String category,
        String brand,
        String createdAt
) {
    public static ProductDetailResponse from(ProductDocument doc) {
        return new ProductDetailResponse(
                doc.getId(),
                doc.getName(),
                doc.getDescription(),
                doc.getPrice(),
                doc.getCategory(),
                doc.getBrand(),
                doc.getCreatedAt()
        );
    }
}