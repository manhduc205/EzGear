package com.manhduc205.ezgear.dtos.request;

import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequest {
    private Long userId;
    private Long branchId;
    private Long shippingAddressId;
    private String paymentMethod;
    private String note;
    private List<CartItemRequest> items;
}
