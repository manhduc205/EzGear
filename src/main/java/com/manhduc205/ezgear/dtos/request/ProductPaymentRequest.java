package com.manhduc205.ezgear.dtos.request;

import lombok.Data;

@Data
public class ProductPaymentRequest {
    private String orderCode;
    private Long amount;
}
