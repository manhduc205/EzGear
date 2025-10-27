package com.manhduc205.ezgear.dtos.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemRequest {
    private Long skuId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
}
