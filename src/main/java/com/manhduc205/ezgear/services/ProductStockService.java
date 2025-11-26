package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.ProductStockDTO;
import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.responses.StockResponse;

import java.util.List;

public interface ProductStockService {
    ProductStockDTO adjustStock(ProductStockDTO productStockDTO, int delta);
    int getAvailable(Long skuId, Long warehouseId);
    void reduceStock(List<CartItemRequest> cartItems, Long warehouseId, Long orderId);
    List<StockResponse> getAllStock();
    void reserveStock(String orderCode, Long skuId, Long branchId, int qty);
    boolean hasReservation(String orderCode);
    void commitReservation(String orderCode);
    void releaseReservation(String orderCode);
    void reduceStockDirect(Long skuId, Long branchId, int qty);
}
