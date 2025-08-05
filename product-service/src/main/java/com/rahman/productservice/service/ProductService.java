package com.rahman.productservice.service;


import com.rahman.productservice.dto.product.ProductResponse;

import java.util.List;

public interface ProductService {
    List<ProductResponse> findAll();
}
