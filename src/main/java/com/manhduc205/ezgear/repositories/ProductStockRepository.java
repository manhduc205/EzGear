package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.ProductStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {
    Optional<ProductStock> findByProductSkuIdAndWarehouseId(Long skuId, Long warehouseId);
}
