package com.manhduc205.ezgear.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockResponse {
    private String sku;             // Mã SKU (ví dụ: MBA15-M3-8-256)
    private String skuName;         // Tên sản phẩm (ví dụ: MacBook Air M3 13 inch 8GB/256GB)
    private String warehouseName;   // Tên kho (ví dụ: Kho chính Hà Nội)
    private Integer qtyOnHand;      // Tồn thực tế
    private Integer qtyReserved;    // Đang giữ
    private Integer safetyStock;    // Tồn an toàn
    private Integer available;      // Có thể bán
}
