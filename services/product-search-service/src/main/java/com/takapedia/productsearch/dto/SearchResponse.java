package com.takapedia.productsearch.dto;

import com.takapedia.productsearch.document.ProductDocument;

import java.util.List;
import java.util.Map;

public record SearchResponse(
        List<ProductDocument> products,
        long totalHits,        // total produk cocok (semua halaman)
        int page,              // halaman sekarang
        int size,              // ukuran halaman
        int totalPages,        // total halaman
        Map<String, Long> brands,
        Map<String, Long> categories,
        Map<String, Long> priceRanges
) {}