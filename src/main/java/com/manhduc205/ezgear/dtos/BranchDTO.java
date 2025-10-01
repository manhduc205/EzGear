package com.manhduc205.ezgear.dtos;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BranchDTO {
    private Long id;
    private String code;
    private String name;
    private String province;
    private String addressLine;
    private String phone;
    private Boolean isActive;
}
