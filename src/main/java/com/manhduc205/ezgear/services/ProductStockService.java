package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.ProductStockDTO;
import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.responses.BranchStockResponse;
import com.manhduc205.ezgear.dtos.responses.StockResponse;

import java.util.List;
import java.util.Map;

public interface ProductStockService {
    ProductStockDTO adjustStock(ProductStockDTO productStockDTO, int delta);
    int getAvailable(Long skuId, Long warehouseId);
    List<StockResponse> getAllStock(Long userId);
    void reserveStock(String orderCode, Long skuId, Long warehouseId, int qty);
    boolean hasReservation(String orderCode);
    void commitReservation(String orderCode);
    void releaseReservation(String orderCode);
    void reduceStockDirect(Long skuId, Long warehouseId, int qty);
    int getAvailableInProvince(Long skuId, Integer provinceId);
    void releaseStock(Long skuId, Long warehouseId, int qty);
    // Hàm dùng cho Xuất kho chuyển đi (Trừ Reserved và OnHand)
    void commitTransferStock(Long skuId, Long warehouseId, int qty);

    // Hàm dùng cho Nhập kho chuyển đến (Cộng OnHand)
    void addStock(Long skuId, Long warehouseId, int qty);
    List<StockResponse> getStockByBranchId(Long branchId);
    // Trả về Map<WarehouseId, Map<SkuId, Qty>> để tra cứu cực nhanh O(1)
    Map<Long, Map<Long, Integer>> getStockMatrix(List<Long> warehouseIds, List<Long> skuIds);
    Map<Long, Integer> reserveStockDistributed(String orderCode, Long skuId, int requestedQty, Long hubWarehouseId, Integer provinceId);
    List<BranchStockResponse> getStockLocations(Long skuId, Integer provinceId, Integer districtId);
}
