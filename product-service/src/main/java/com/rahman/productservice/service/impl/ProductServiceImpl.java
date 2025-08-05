package com.rahman.productservice.service.impl;

import com.rahman.productservice.dto.product.ProductResponse;
import com.rahman.productservice.entity.Product;
import com.rahman.productservice.mapper.ProductMapper;
import com.rahman.productservice.repository.ProductRepository;
import com.rahman.productservice.service.ProductService;
import com.rahman.productservice.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ValidationService validationService;
    private final ProductMapper productMapper;
    private final MessageSource messageSource;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository, ValidationService validationService,
                              ProductMapper productMapper, MessageSource messageSource) {
        this.productRepository = productRepository;
        this.validationService = validationService;
        this.productMapper = productMapper;
        this.messageSource = messageSource;
    }

    @Override
    public List<ProductResponse> findAll() {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .map(productMapper::toResponse)
                .toList();
    }
}
