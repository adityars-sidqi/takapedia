package com.rahman.productservice.repository;

import com.rahman.productservice.entity.ProductBundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductBundleRepository extends JpaRepository<ProductBundle, UUID> {
}