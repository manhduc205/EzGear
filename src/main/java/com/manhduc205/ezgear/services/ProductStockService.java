package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.ProductStockDTO;
import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.responses.StockResponse;

import java.util.List;

public interface ProductStockService {
    ProductStockDTO adjustStock(ProductStockDTO productStockDTO, int delta);
    int getAvailable(Long skuId, Long warehouseId);
    List<StockResponse> getAllStock();
    void reserveStock(String orderCode, Long skuId, Long warehouseId, int qty);
    boolean hasReservation(String orderCode);
    void commitReservation(String orderCode);
    void releaseReservation(String orderCode);
    void reduceStockDirect(Long skuId, Long warehouseId, int qty);
    int getAvailableInProvince(Long skuId, Integer provinceId);
    int getTotalSystemStock(Long skuId);
    // Hàm dùng cho Xuất kho chuyển đi (Trừ Reserved và OnHand)
    void commitTransferStock(Long skuId, Long warehouseId, int qty);

    // Hàm dùng cho Nhập kho chuyển đến (Cộng OnHand)
    void addStock(Long skuId, Long warehouseId, int qty);
}
