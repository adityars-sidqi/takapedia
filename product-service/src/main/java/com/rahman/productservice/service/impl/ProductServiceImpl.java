package com.rahman.productservice.service.impl;

import com.rahman.productservice.dto.product.CreateProductRequest;
import com.rahman.productservice.dto.product.ProductResponse;
import com.rahman.productservice.dto.product.UpdateProductRequest;
import com.rahman.productservice.entity.Category;
import com.rahman.productservice.entity.Product;
import com.rahman.productservice.entity.ProductTag;
import com.rahman.productservice.entity.Tag;
import com.rahman.productservice.exception.BadRequestException;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.rahman.productservice.constants.MessagesCodeConstant.*;

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
    public ProductResponse findById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(messageSource.getMessage(PRODUCT_NOT_FOUND, null, LocaleContextHolder.getLocale())));

        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse save(CreateProductRequest createProductRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("[USER: {}] Attempt to save product", username);
        log.info("[USER: {}] Start saving product", username);
        log.debug("[USER: {}] Request Payload: {}", username, createProductRequest);

        validationService.validate(createProductRequest);

        // Ambil Category dari database
        log.debug("[USER: {}] Fetching category with ID: {}", username, createProductRequest.categoryId());
        Category category = categoryRepository.findById(createProductRequest.categoryId())
                .orElseThrow(() -> {
                    log.warn("[USER: {}] Category not found: {}", username, createProductRequest.categoryId());
                    return new ResourceNotFoundException(
                            messageSource.getMessage(CATEGORY_NOT_FOUND, null, LocaleContextHolder.getLocale()));
                });

        // Ambil semua Tag berdasarkan ID
        log.debug("[USER: {}] Fetching tags with IDs: {}", username, createProductRequest.tagIds());
        List<Tag> tags = tagRepository.findAllById(createProductRequest.tagIds());

        if (tags.size() != createProductRequest.tagIds().size()) {
            log.warn("[USER: {}] Some tag IDs not found. Request: {}, Found: {}", username, createProductRequest.tagIds(), tags.size());
            throw new ResourceNotFoundException(
                    messageSource.getMessage(TAG_NOT_FOUND, null, LocaleContextHolder.getLocale())
            );
        }

        // Mapping dari DTO ke Entity
        Product product = productMapper.toEntity(createProductRequest, category, tags);

        // Simpan ke DB
        productRepository.save(product);
        log.info("[USER: {}] Product saved successfully. Product ID: {}", username, product.getId());

        // Mapping ke response
        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse update(UUID id, UpdateProductRequest updateProductRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("[USER: {}] Attempt to update product with ID: {}", username, id);
        log.info("[USER: {}] Start updating product with ID: {}", username, id);
        log.debug("[USER: {}] Request Payload: {}", username, updateProductRequest);

        if (updateProductRequest.name() == null &&
                updateProductRequest.description() == null &&
                updateProductRequest.price() == null &&
                updateProductRequest.stock() == null &&
                updateProductRequest.categoryId() == null &&
                updateProductRequest.tagIds() == null) {
            throw new BadRequestException("At least one field must be provided to update product.");
        }

        log.debug("[USER: {}] Fetching Product with ID: {}", username, id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[USER: {}][UPDATE FAILED] Product with ID: {} not found", username, id);
                    return new ResourceNotFoundException(
                        messageSource.getMessage(PRODUCT_NOT_FOUND, null, LocaleContextHolder.getLocale())
                    );
                });

        // Update name jika tidak null dan tidak kosong
        Optional.ofNullable(updateProductRequest.name())
                .filter(name -> !name.isBlank())
                .ifPresent(product::setName);

        // Update description jika tidak null dan tidak kosong
        Optional.ofNullable(updateProductRequest.description())
                .filter(desc -> !desc.isBlank())
                .ifPresent(product::setDescription);

        Optional.ofNullable(updateProductRequest.price())
                .ifPresent(product::setPrice);

        Optional.ofNullable(updateProductRequest.stock())
                .ifPresent(product::setStock);

        if (updateProductRequest.categoryId() != null) {

            log.debug("[USER: {}] Fetching category with ID: {}", username, updateProductRequest.categoryId());
            Category category = categoryRepository.findById(updateProductRequest.categoryId())
                    .orElseThrow(() -> {
                        log.warn("[USER: {}][UPDATE FAILED] Category with ID: {} not found", username, updateProductRequest.categoryId());
                        return new ResourceNotFoundException(
                                    messageSource.getMessage(CATEGORY_NOT_FOUND, null, LocaleContextHolder.getLocale())
                        );
                    });

            product.setCategory(category);
        }

        if (updateProductRequest.tagIds() != null) {

            // Hapus relasi tag lama
            product.getProductTags().clear();

            // Ambil tag baru berdasarkan id
            List<Tag> tags = tagRepository.findAllById(updateProductRequest.tagIds());

            if (tags.size() != updateProductRequest.tagIds().size()) {
                log.warn("[USER: {}][UPDATE FAILED] Some tag IDs not found. Request: {}, Found: {}", username, updateProductRequest.tagIds(), tags.size());
                throw new ResourceNotFoundException(
                        messageSource.getMessage(TAG_NOT_FOUND, null, LocaleContextHolder.getLocale())
                );
            }

            // Buat relasi baru
            Set<ProductTag> newProductTags = tags.stream()
                    .map(tag -> new ProductTag(product, tag))
                    .collect(Collectors.toSet());

            // Set relasi baru ke product
            product.getProductTags().addAll(newProductTags);

        }

        Product updated  = productRepository.save(product);

        log.info("[USER: {}][UPDATE SUCCESS] Product with ID: {} has been updated", username, id);

        return productMapper.toResponse(updated);
    }

    @Override
    public void deleteById(UUID id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("[USER: {}] Attempt to delete product with ID: {}", username, id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[USER: {}][DELETE FAILED] Product with ID: {} not found", username, id);
                    return new ResourceNotFoundException(
                            messageSource.getMessage(PRODUCT_NOT_FOUND, null, LocaleContextHolder.getLocale())
                    );
                });

        productRepository.delete(product);
        log.info("[USER: {}][DELETE SUCCESS] Product with ID: {} has been deleted", username, id);
    }
}
