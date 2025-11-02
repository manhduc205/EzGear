package com.manhduc205.ezgear.dtos.request;

import lombok.Data;

@Data
public class CustomerAddressRequest {
    private Long userId;
    private String receiverName;
    private String receiverPhone;
    private String locationCode;
    private String addressLine;
    private String label;
    private Boolean isDefault;
}
