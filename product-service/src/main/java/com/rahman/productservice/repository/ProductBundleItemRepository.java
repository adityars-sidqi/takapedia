package com.rahman.productservice.repository;

import com.rahman.productservice.entity.ProductBundleItem;
import com.rahman.productservice.entity.ProductBundleItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductBundleItemRepository extends JpaRepository<ProductBundleItem, ProductBundleItemId> {
}