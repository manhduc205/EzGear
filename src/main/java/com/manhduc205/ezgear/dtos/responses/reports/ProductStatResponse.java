package com.manhduc205.ezgear.dtos.responses.reports;

import lombok.*;

@Data
@AllArgsConstructor
public class ProductStatResponse {
    private Long productId;
    private String name;
    private String sku;
    private Long soldQty;      // Số lượng đã bán
    private Long stockQty;     // Số lượng tồn kho
    private Double revenue;    // Doanh thu mang lại
    private Integer daysNoSale; // Số ngày chưa bán được (cho Dead Stock)
}
