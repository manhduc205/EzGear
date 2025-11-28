package com.manhduc205.ezgear.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPaymentRequest {
    private String orderCode;
    private Long amount;
    private String ipAddr;
}