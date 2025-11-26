package com.manhduc205.ezgear.dtos.request.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {
    private Long skuId;
    private Long productId;
    private String productNameSnapshot;
    private String skuNameSnapshot;
    private String imageUrlSnapshot;
    private Integer quantity;
    private Long unitPrice;
    private Long discountAmount;
    private Long lineTotal;
}
