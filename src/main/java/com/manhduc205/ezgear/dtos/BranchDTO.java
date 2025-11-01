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
    private Integer locationCode;
    private String addressLine;
    private String phone;
    private Boolean isActive;
}
