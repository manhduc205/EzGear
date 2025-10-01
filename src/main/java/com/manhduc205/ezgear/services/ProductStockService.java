package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.ProductStockDTO;
import com.manhduc205.ezgear.models.ProductStock;

public interface ProductStockService {
    ProductStock adjustStock(ProductStockDTO productStockDTO, int delta);
    int getAvailable(Long skuId, Long warehouseId);
}
