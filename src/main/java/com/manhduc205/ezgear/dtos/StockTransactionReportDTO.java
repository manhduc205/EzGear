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
    private String imageUrl;         // URL ảnh sản phẩm
    private String productVariant;   // Tên sản phẩm + option
    private String sku;              // Mã SKU
    private String warehouseName;    // Tên kho
    private String transactionType;  // Nhập kho / Xuất kho / Điều chỉnh
    private LocalDateTime time;      // Thời gian giao dịch
    private Integer quantity;        // Số lượng thay đổi (+/-)
    private Integer stockBefore;     // Tồn trước giao dịch
    private Integer stockAfter;      // Tồn sau giao dịch
    private Long purchasePrice;// Giá nhập
    private Long retailPrice;  // Giá bán hiện tại
    private String agent;            // Người thực hiện
}

