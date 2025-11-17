package com.manhduc205.ezgear.dtos.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckoutItemResponse {
    private Long skuId;
    private String productName;
    private String skuName;
    private Integer quantity;
    private Long unitPrice;
    private Long lineTotal;
}
