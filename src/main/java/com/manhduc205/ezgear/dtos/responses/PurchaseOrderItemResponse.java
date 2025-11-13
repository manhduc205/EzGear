package com.manhduc205.ezgear.dtos.responses;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItemResponse {

    private String sku;
    private String skuName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;     // quantity * unitPrice
}
