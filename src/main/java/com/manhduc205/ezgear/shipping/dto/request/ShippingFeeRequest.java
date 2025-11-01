package com.manhduc205.ezgear.shipping.dto.request;

import lombok.Data;

@Data
public class ShippingFeeRequest {
    private Long branchId;
    private Long addressId;
    private Long skuId;
}
