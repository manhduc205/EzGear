package com.manhduc205.ezgear.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseDTO {
    private Long id;
    private Long branchId;
    private String code;
    private String name;
    private Boolean isActive;
}
