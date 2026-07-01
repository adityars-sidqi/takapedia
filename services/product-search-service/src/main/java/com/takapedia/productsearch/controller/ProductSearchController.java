package com.takapedia.productsearch.controller;

import com.takapedia.productsearch.document.ProductDocument;
import com.takapedia.productsearch.repository.ProductSearchRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductSearchController {

    private final ProductSearchRepository repository;

    public ProductSearchController(ProductSearchRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/v1/products/search")
    public List<ProductDocument> search(@RequestParam String q) {
        return repository.findByNameContaining(q);
    }
}