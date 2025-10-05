package com.manhduc205.ezgear.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockTransactionReportDTO {
    private String imageUrl;
    private String productVariant;   // tên + option
    private String sku;
    private String barcode;
    private LocalDateTime time;
    private Integer quantity;

    private Integer reserved;        // từ ProductStock.qty_reserved
    private Integer buffer;          // từ ProductStock.safety_stock
    private Integer available;       // qty_on_hand - qty_reserved - safety_stock

    private BigDecimal purchasePrice; // từ PO hoặc ref_id
    private BigDecimal retailPrice;   // từ ProductSKU.price

    private String agent;            // từ AuditLog.actor_id → Users.name
}

