package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.StockTransactionReportDTO;
import com.manhduc205.ezgear.models.*;
import com.manhduc205.ezgear.repositories.*;
import com.manhduc205.ezgear.services.ProductService;
import com.manhduc205.ezgear.services.StockTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockTransactionServiceImpl implements StockTransactionService {
    private final StockTransactionRepository stockTransactionRepository;
    private final ProductStockRepository productStockRepository;
    private final ProductSkuRepository productSkuRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    @Override
    public List<StockTransactionReportDTO> getTransactionReports(Long skuId, Long warehouseId) {
        List<StockTransaction> transactions;
        if (skuId != null && warehouseId != null) {
            transactions = stockTransactionRepository.findBySkuIdAndWarehouseId(skuId, warehouseId);
        } else {
            transactions = stockTransactionRepository.findAll();
        }
        return transactions.stream().map(tx -> {
            ProductSKU sku = productSkuRepository.findById(tx.getSkuId()).orElse(null);
            ProductStock stock = productStockRepository.findByProductSkuIdAndWarehouseId(
                    tx.getSkuId(), tx.getWarehouseId()).orElse(null);
            AuditLog audit = auditLogRepository.findTopByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    "StockTransaction", tx.getId());
            String imageUrl = sku.getProduct().getImageUrl();

            String agent = audit != null
                    ? userRepository.findById(audit.getActorId()).map(User::getUsername).orElse("System")
                    : "System";

            return StockTransactionReportDTO.builder()
                    .imageUrl(imageUrl)
                    .productVariant(sku != null ? sku.getName() : "N/A")
                    .sku(sku != null ? sku.getSku() : "")
                    .barcode(sku != null ? sku.getBarcode() : "")
                    .time(tx.getCreatedAt())
                    .quantity(tx.getQuantity())
                    .reserved(stock != null ? stock.getQtyReserved() : 0)
                    .buffer(stock != null ? stock.getSafetyStock() : 0)
                    .available(stock != null
                            ? stock.getQtyOnHand() - stock.getQtyReserved() - stock.getSafetyStock()
                            : 0)
                    .purchasePrice(BigDecimal.ZERO) // TODO: trace từ PO theo refId
                    .retailPrice(sku != null ? sku.getPrice() : BigDecimal.ZERO)
                    .agent(agent)
                    .build();
        }).toList();
    }

}
