package com.manhduc205.ezgear.dtos.responses;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ShippingAddressInfo {
    private Long id;
    private String fullAddress;
    private Boolean isDefault;
}

