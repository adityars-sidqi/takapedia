package com.takapedia.productsearch.controller;

import com.takapedia.productsearch.dto.ProductDetailResponse;
import com.takapedia.productsearch.dto.SearchResponse;
import com.takapedia.productsearch.service.ProductSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductSearchController {

    private final ProductSearchService service;

    public ProductSearchController(ProductSearchService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/products/search")
    public SearchResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        return service.search(q, brand, category, minPrice, maxPrice, page, size, sort);
    }

    @GetMapping("/api/v1/products")
    public SearchResponse list(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        return service.search(null, brand, category, minPrice, maxPrice, page, size, sort);
    }

    @GetMapping("/api/v1/products/{id}")
    public ProductDetailResponse getById(@PathVariable String id) {
        return service.getById(id);
    }
}