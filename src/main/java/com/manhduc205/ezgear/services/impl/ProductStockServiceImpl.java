package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.ProductStockDTO;
import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.responses.StockResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.models.ProductStock;
import com.manhduc205.ezgear.models.Warehouse;
import com.manhduc205.ezgear.repositories.OrderRepository;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
import com.manhduc205.ezgear.repositories.ProductStockRepository;
import com.manhduc205.ezgear.repositories.WarehouseRepository;
import com.manhduc205.ezgear.services.ProductStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductStockServiceImpl implements ProductStockService {

    private final ProductStockRepository productStockRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductSkuRepository productSkuRepository;
    private final OrderRepository orderRepository;

    //Input: ProductStockDTO + delta (số lượng cộng/trừ).
    @Override
    public ProductStockDTO adjustStock(ProductStockDTO productStockDTO, int delta) {
        ProductStock productStock = productStockRepository.findByProductSkuIdAndWarehouseId(productStockDTO.getSkuId(),productStockDTO.getWarehouseId())
                .orElseGet(() ->{
                    ProductSKU sku = productSkuRepository.findById(productStockDTO.getSkuId())
                            .orElseThrow(() -> new RequestException("SKU not found"));
                    Warehouse warehouse = warehouseRepository.findById(productStockDTO.getWarehouseId())
                            .orElseThrow(() -> new RequestException("Warehouse not found"));
                    return ProductStock.builder()
                            .productSku(sku)
                            .warehouse(warehouse)
                            .qtyOnHand(0)
                            .qtyReserved(0)
                            .safetyStock(productStockDTO.getSafetyStock() != null ? productStockDTO.getSafetyStock() : 0)
                            .build();
                });
        productStock.setQtyOnHand(productStock.getQtyOnHand() + delta);
        ProductStock saved = productStockRepository.save(productStock);
        return ProductStockDTO.builder()
                .id(saved.getId())
                .skuId(saved.getProductSku().getId())
                .warehouseId(saved.getWarehouse().getId())
                .qtyOnHand(saved.getQtyOnHand())
                .qtyReserved(saved.getQtyReserved())
                .safetyStock(saved.getSafetyStock())
                .build();
    }

    @Override
    public int getAvailable(Long skuId, Long warehouseId) {
        return productStockRepository.findByProductSkuIdAndWarehouseId(skuId, warehouseId)
                .map(stock -> {
                    int available = stock.getQtyOnHand() - stock.getQtyReserved() - stock.getSafetyStock();
                    return Math.max(0, available); // Không bao giờ trả về số âm
                })
                .orElse(0);
    }


    @Override
    public List<StockResponse> getAllStock() {
        return productStockRepository.findAll()
                .stream()
                .map(stock -> StockResponse.builder()
                        .sku(stock.getProductSku().getSku())
                        .skuName(stock.getProductSku().getName())
                        .warehouseName(stock.getWarehouse().getName())
                        .qtyOnHand(stock.getQtyOnHand())
                        .qtyReserved(stock.getQtyReserved())
                        .safetyStock(stock.getSafetyStock())
                        .available(stock.getQtyOnHand() - stock.getQtyReserved() - stock.getSafetyStock())
                        .build()
                )
                .toList();
    }

    @Override
    @Transactional
    public void reserveStock(String orderCode, Long skuId, Long branchId, int qty) {
        if (branchId == null) {
            throw new RequestException("Kho không hợp lệ");
        }
        if (qty <= 0) {
            throw new RequestException("Số lượng giữ chỗ không hợp lệ");
        }

        int updated = productStockRepository.reserveStock(skuId, branchId, qty);
        if (updated == 0) {
            throw new RequestException("Không đủ tồn kho để giữ chỗ");
        }
        // Reservation is implicitly tracked per order by OrderItem quantities
    }

    @Override
    public boolean hasReservation(String orderCode) {
        // Simple implementation: if order is in WAITING_PAYMENT and has items, we treat it as reserved.
        return orderRepository.findByCode(orderCode)
                .map(o -> "WAITING_PAYMENT".equalsIgnoreCase(o.getStatus()) && o.getItems() != null && !o.getItems().isEmpty())
                .orElse(false);
    }

    @Override
    @Transactional
    //Chốt đơn / Xuất kho thật
    public void commitReservation(String orderCode) {
        var order = orderRepository.findByCode(orderCode)
                .orElseThrow(() -> new RequestException("Order not found when committing reservation"));

        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }

        for (var it : order.getItems()) {
            int updated = productStockRepository.commitReserved(it.getSkuId(), order.getBranchId(), it.getQuantity());
            if (updated == 0) {
                throw new RequestException("Không thể commit giữ chỗ cho SKU " + it.getSkuId());
            }
        }
    }

    @Override
    @Transactional
    //Nhả hàng / Hủy giữ chỗ
    public void releaseReservation(String orderCode) {
        var order = orderRepository.findByCode(orderCode)
                .orElseThrow(() -> new RequestException("Order not found when releasing reservation"));

        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }

        for (var it : order.getItems()) {
            productStockRepository.releaseReserved(it.getSkuId(), order.getBranchId(), it.getQuantity());
        }
    }

    @Override
    @Transactional
    //Trừ kho trực tiếp - Không giữ chỗ
    public void reduceStockDirect(Long skuId, Long branchId, int qty) {
        if (branchId == null) {
            throw new RequestException("Kho không hợp lệ");
        }
        if (qty <= 0) {
            throw new RequestException("Số lượng trừ kho không hợp lệ");
        }
        int updated = productStockRepository.reduceDirect(skuId, branchId, qty);
        if (updated == 0) {
            throw new RequestException("Không đủ tồn kho để trừ trực tiếp");
        }
    }

    @Override
    public int getAvailableInProvince(Long skuId, Integer provinceId) {
        // Nếu không có ProvinceId (Lỗi Frontend hoặc chưa chọn), coi như không có hàng tại đó
        if (provinceId == null) {
            return 0;
        }
        Integer totalAvailable = productStockRepository.sumStockByProvince(skuId, provinceId);
        return totalAvailable == null ? 0 : Math.max(0, totalAvailable);
    }

    @Override
    public int getTotalSystemStock(Long skuId) {
        Integer total = productStockRepository.sumTotalAvailable(skuId);
        return total == null ? 0 : Math.max(0, total);
    }
}
