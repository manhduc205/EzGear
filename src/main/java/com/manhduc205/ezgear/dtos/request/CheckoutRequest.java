package com.manhduc205.ezgear.dtos.request;

import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequest {
    private List<CartItemRequest> cartItems;
    private Long addressId;
    private String voucherCode;
    private String paymentMethod;
    private Integer serviceId;
}
