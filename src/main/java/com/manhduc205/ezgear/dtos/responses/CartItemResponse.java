package com.manhduc205.ezgear.dtos.responses;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartItemResponse {
    private Long skuId;
    private String productName;
    private String imageUrl;
    private Long price;
    private int quantity;
    private boolean selected;
}
