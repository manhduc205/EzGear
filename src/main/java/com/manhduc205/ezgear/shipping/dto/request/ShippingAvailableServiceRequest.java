package com.manhduc205.ezgear.shipping.dto.request;

import lombok.Data;

@Data
public class ShippingAvailableServiceRequest {
    private Long branchId;
    private Long addressId;
}
