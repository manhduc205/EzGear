package com.manhduc205.ezgear.models.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private Long skuId;
    private String productName;
    private String imageUrl;
    private Integer quantity;
    private Double price;
    private Boolean selected = true;
}

