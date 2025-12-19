package com.manhduc205.ezgear.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddressDTO {
    private Long id;

    private boolean isDefault;
    private Integer provinceId;
    private Integer districtId;
    private String wardCode;
    private String addressLine;
    private String fullAddress;
    private String receiverName;
    private String receiverPhone;
    private String label;
}
