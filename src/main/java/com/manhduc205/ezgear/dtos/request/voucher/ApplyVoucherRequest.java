package com.manhduc205.ezgear.dtos.request.voucher;

import lombok.Data;

import java.util.List;

@Data
public class ApplyVoucherRequest {
    private String code;
    private Long subtotal;
    private Long shippingFee;
    private List<ApplyVoucherItemRequest> items;
}

