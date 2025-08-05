package com.rahman.productservice.controller;

import com.rahman.commonlib.ApiResponse;
import com.rahman.productservice.dto.product.ProductResponse;
import com.rahman.productservice.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<List<ProductResponse>> findAll() {
        return ApiResponse.success(productService.findAll());
    }
}
