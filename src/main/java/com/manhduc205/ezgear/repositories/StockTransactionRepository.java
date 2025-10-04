package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    List<StockTransaction> findBySkuIdAndWarehouseId(Long skuId, Long warehouseId);
    List<StockTransaction> findByRefTypeAndRefId(String refType, Long refId);
}
