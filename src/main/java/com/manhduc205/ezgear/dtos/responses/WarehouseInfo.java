package com.manhduc205.ezgear.dtos.responses;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class WarehouseInfo {
    private Long id;
    private String name;
}

