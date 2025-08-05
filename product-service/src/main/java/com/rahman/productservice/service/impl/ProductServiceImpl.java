package com.rahman.productservice.service.impl;

import com.rahman.productservice.dto.product.CreateProductRequest;
import com.rahman.productservice.dto.product.ProductResponse;
import com.rahman.productservice.entity.Category;
import com.rahman.productservice.entity.Product;
import com.rahman.productservice.entity.Tag;
import com.rahman.productservice.exception.ResourceNotFoundException;
import com.rahman.productservice.mapper.ProductMapper;
import com.rahman.productservice.repository.CategoryRepository;
import com.rahman.productservice.repository.ProductRepository;
import com.rahman.productservice.repository.TagRepository;
import com.rahman.productservice.service.ProductService;
import com.rahman.productservice.service.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.rahman.productservice.constants.MessagesCodeConstant.CATEGORY_NOT_FOUND;
import static com.rahman.productservice.constants.MessagesCodeConstant.TAG_NOT_FOUND;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ValidationService validationService;
    private final ProductMapper productMapper;
    private final MessageSource messageSource;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository, TagRepository tagRepository, ValidationService validationService,
                              ProductMapper productMapper, MessageSource messageSource) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
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

    @Override
    public ProductResponse save(CreateProductRequest createProductRequest) {
        log.info("Start saving product");
        log.debug("Request Payload: {}", createProductRequest);

        validationService.validate(createProductRequest);

        // Ambil Category dari database
        log.debug("Fetching category with ID: {}", createProductRequest.categoryId());
        Category category = categoryRepository.findById(createProductRequest.categoryId())
                .orElseThrow(() -> {
                    log.warn("Category not found: {}", createProductRequest.categoryId());
                    return new ResourceNotFoundException(
                            messageSource.getMessage(CATEGORY_NOT_FOUND, null, LocaleContextHolder.getLocale()));
                });

        // Ambil semua Tag berdasarkan ID
        log.debug("Fetching tags with IDs: {}", createProductRequest.tagIds());
        List<Tag> tags = tagRepository.findAllById(createProductRequest.tagIds());

        if (tags.size() != createProductRequest.tagIds().size()) {
            log.warn("Some tag IDs not found. Request: {}, Found: {}", createProductRequest.tagIds(), tags.size());
            throw new ResourceNotFoundException(
                    messageSource.getMessage(TAG_NOT_FOUND, null, LocaleContextHolder.getLocale())
            );
        }

        // Mapping dari DTO ke Entity
        Product product = productMapper.toEntity(createProductRequest, category, tags);

        // Simpan ke DB
        productRepository.save(product);
        log.info("Product saved successfully. Product ID: {}", product.getId());

        // Mapping ke response
        return productMapper.toResponse(product);
    }
}
