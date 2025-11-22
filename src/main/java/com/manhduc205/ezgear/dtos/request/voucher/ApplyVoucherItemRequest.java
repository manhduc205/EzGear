package com.manhduc205.ezgear.dtos.request.voucher;

import lombok.Data;

@Data
public class ApplyVoucherItemRequest {
    private Long skuId;
    private Long productId;
    private Long categoryId;
    private Long price;
    private Integer quantity;
}

