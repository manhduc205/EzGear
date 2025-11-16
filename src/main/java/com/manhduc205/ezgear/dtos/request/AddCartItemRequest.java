package com.manhduc205.ezgear.dtos.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCartItemRequest {
    private Long skuId;
    private int quantity;
}

