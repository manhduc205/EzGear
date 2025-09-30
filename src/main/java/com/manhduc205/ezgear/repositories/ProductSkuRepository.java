package com.manhduc205.ezgear.repositories;


import com.manhduc205.ezgear.models.ProductSKU;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductSkuRepository extends JpaRepository<ProductSKU,Long>, JpaSpecificationExecutor<ProductSKU> {
    boolean existsBySku(String sku);
}

