package com.takapedia.product.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException  {

    public ProductNotFoundException(UUID productId) {
        super("Product tidak ditemukan: " + productId);
    }
}
