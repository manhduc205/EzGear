package com.manhduc205.ezgear.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddressDTO {
    private Long id;
    private String fullAddress;
    private boolean isDefault;
}

