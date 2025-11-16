package com.manhduc205.ezgear.dtos.request;

import lombok.Data;

@Data
public class AddToCartRequest {
    private Long skuId;
    private int quantity;
}

