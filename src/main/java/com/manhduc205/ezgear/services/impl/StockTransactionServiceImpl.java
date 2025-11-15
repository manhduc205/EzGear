package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.StockTransactionReportDTO;
import com.manhduc205.ezgear.models.*;
import com.manhduc205.ezgear.repositories.*;
import com.manhduc205.ezgear.services.StockTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockTransactionServiceImpl implements StockTransactionService {

    private final StockTransactionRepository stockTransactionRepository;
    private final ProductSkuRepository productSkuRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;

    @Override
    public List<StockTransactionReportDTO> getTransactionReports(Long skuId, Long warehouseId) {
        List<StockTransaction> transactions;

        if (skuId != null && warehouseId != null) {
            transactions = stockTransactionRepository.findBySkuIdAndWarehouseId(skuId, warehouseId);
        } else if (skuId != null) {
            transactions = stockTransactionRepository.findBySkuId(skuId);
        } else if (warehouseId != null) {
            transactions = stockTransactionRepository.findByWarehouseId(warehouseId);
        } else {
            transactions = stockTransactionRepository.findAll();
        }

        return transactions.stream().map(tx -> {
            ProductSKU sku = productSkuRepository.findById(tx.getSkuId()).orElse(null);
            Warehouse warehouse = warehouseRepository.findById(tx.getWarehouseId()).orElse(null);

            // Lấy người thực hiện
            String agent = userRepository.findById(tx.getCreatedBy())
                    .map(u -> u.getFullName() != null ? u.getFullName() : u.getEmail())
                    .orElse("System");


            // Loại giao dịch
            String transactionType = getTransactionType(tx);

            return StockTransactionReportDTO.builder()
                    .imageUrl(sku != null && sku.getProduct() != null ? sku.getProduct().getImageUrl() : null)
                    .productVariant(sku != null ? sku.getName() : "N/A")
                    .sku(sku != null ? sku.getSku() : "")
                    .warehouseName(warehouse != null ? warehouse.getName() : "N/A")
                    .transactionType(transactionType)
                    .time(tx.getCreatedAt())
                    .quantity(tx.getDirection() == StockTransaction.Direction.OUT
                            ? -Math.abs(tx.getQuantity())
                            : Math.abs(tx.getQuantity()))
                    .stockBefore(tx.getStockBefore())
                    .stockAfter(tx.getStockAfter())
                    .purchasePrice(tx.getPurchasePrice() != null ? tx.getPurchasePrice() : BigDecimal.ZERO)
                    .retailPrice(sku != null ? sku.getPrice() : BigDecimal.ZERO)
                    .agent(agent)
                    .build();
        }).toList();
    }

    private String getTransactionType(StockTransaction tx) {
        if (tx.getRefType() == null) {
            return tx.getDirection() == StockTransaction.Direction.IN ? "Nhập kho" : "Xuất kho";
        }
        switch (tx.getRefType().toUpperCase()) {
            case "PO":
                return "Nhập kho";
            case "SO":
                return "Xuất kho";
            case "ADJUST":
                return "Điều chỉnh";
            case "TRANSFER":
                return "Chuyển kho";
            default:
                return tx.getDirection() == StockTransaction.Direction.IN ? "Nhập kho" : "Xuất kho";
        }
    }
}
