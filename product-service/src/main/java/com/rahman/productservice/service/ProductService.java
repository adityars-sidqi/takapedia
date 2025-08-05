package com.rahman.productservice.service;


import com.rahman.productservice.dto.product.CreateProductRequest;
import com.rahman.productservice.dto.product.ProductResponse;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    List<ProductResponse> findAll();
    ProductResponse save(CreateProductRequest createProductRequest);
    void deleteById(UUID id);
}
