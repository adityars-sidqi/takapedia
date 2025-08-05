package com.rahman.productservice.service;


import com.rahman.productservice.dto.product.CreateProductRequest;
import com.rahman.productservice.dto.product.ProductResponse;
import com.rahman.productservice.dto.product.UpdateProductRequest;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    List<ProductResponse> findAll();
    ProductResponse save(CreateProductRequest createProductRequest);
    ProductResponse update(UUID id, UpdateProductRequest updateProductRequest);
    void deleteById(UUID id);
}
