package com.manhduc205.ezgear.repositories;


import com.manhduc205.ezgear.models.ProductSKU;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductSkuRepository extends JpaRepository<ProductSKU,Long>, JpaSpecificationExecutor<ProductSKU> {
    boolean existsBySku(String sku);
    List<ProductSKU> findByProductIdAndIsActiveTrueOrderByPriceAsc(Long productId);
    @Modifying
    @Query("UPDATE ProductSKU s SET s.isActive = false WHERE s.product.id = :productId")
    void softDeleteByProductId(Long productId);

    Optional<ProductSKU> findBySku(String sku);

    Optional<ProductSKU> findByBarcode(String barcode);
}

