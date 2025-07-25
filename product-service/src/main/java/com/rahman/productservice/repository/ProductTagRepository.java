package com.rahman.productservice.repository;

import com.rahman.productservice.entity.ProductTag;
import com.rahman.productservice.entity.ProductTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductTagRepository extends JpaRepository<ProductTag, ProductTagId> {
}