package com.manhduc205.ezgear.dtos.request;

import lombok.Data;

@Data
public class CustomerAddressRequest {
    private Long userId;
    private String receiverName;
    private String receiverPhone;
    private Integer provinceId;
    private Integer districtId;
    private String wardCode;
    private String addressLine;
    private String label;
    private Boolean isDefault;
}
