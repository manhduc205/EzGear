package com.manhduc205.ezgear.dtos.request;

import lombok.Data;

import java.util.List;

@Data
public class CartCheckoutRequest {
    private List<CartItemRequest> cartItems;
    private String voucherCode;
    private Integer provinceId;
}